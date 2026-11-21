package com.dess.assessment.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultStatus;

/**
 * Integration tests of the assessment JPA repositories with real PostgreSQL.
 *
 * <p>The assessment table references a course and the result table
 * references a student, so the test inserts one course and one
 * student before the tests run.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class AssessmentPersistenceTest
{
    private static final UUID COURSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final long STUDENT_ID = 5001L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dess_test")
            .withUsername("dess")
            .withPassword("dess");

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private JpaAssessmentRepository assessments;
    private JpaResultRepository results;

    @BeforeAll
    static void migrateAndStartJpa() throws Exception
    {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:../db-migration/src/main/resources/db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement())
        {
            statement.executeUpdate("INSERT INTO app_user (id, email, last_name, first_name, "
                    + "role, active) VALUES (5001, 'student5001@example.com', 'Test', "
                    + "'Student', 'STUDENT', true)");
            statement.executeUpdate("INSERT INTO student (id, registration_number) "
                    + "VALUES (5001, 'REG-5001')");
            statement.executeUpdate("INSERT INTO course (id, code, title, active, created_at) "
                    + "VALUES ('11111111-1111-1111-1111-111111111111', 'MATH101', "
                    + "'Mathematics', true, now())");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", POSTGRES.getUsername());
        properties.put("jakarta.persistence.jdbc.password", POSTGRES.getPassword());
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        entityManagerFactory = Persistence.createEntityManagerFactory("assessmentTestPU", properties);
    }

    @AfterAll
    static void closeJpa()
    {
        if (entityManagerFactory != null)
        {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void open()
    {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        assessments = new JpaAssessmentRepository(entityManager);
        results = new JpaResultRepository(entityManager);
    }

    @AfterEach
    void close()
    {
        if (entityManager.getTransaction().isActive())
        {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    @Test
    void savesAndFindsAssessmentAndResult()
    {
        Assessment savedAssessment = assessments.save(Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, new BigDecimal("20")));
        entityManager.flush();

        Result savedResult = results.save(Result.create(savedAssessment.id(), null, STUDENT_ID,
                new BigDecimal("15.50"), ResultStatus.SCORED, "professor@example.com"));
        entityManager.flush();

        Assessment foundAssessment = assessments.findById(savedAssessment.id()).orElseThrow();
        assertEquals("Final exam", foundAssessment.title());
        assertEquals(new BigDecimal("20"), foundAssessment.maximumScore());
        assertNotNull(foundAssessment.createdAt());
        assertEquals(1, assessments.findByCourseId(COURSE_ID).size());

        Result foundResult = results.findById(savedResult.id()).orElseThrow();
        assertEquals(new BigDecimal("15.50"), foundResult.score());
        assertEquals(ResultStatus.SCORED, foundResult.status());
        assertEquals("professor@example.com", foundResult.enteredBy());
        assertNotNull(foundResult.enteredAt());
        assertNotNull(foundResult.updatedAt());
        assertTrue(results.existsByAssessmentAndStudent(savedAssessment.id(), STUDENT_ID));
        assertEquals(1, results.findByAssessment(savedAssessment.id()).size());
        assertEquals(1, results.findByStudent(STUDENT_ID).size());
    }

    @Test
    void databaseRejectsDuplicateResult()
    {
        Assessment savedAssessment = assessments.save(Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, new BigDecimal("20")));
        entityManager.flush();
        results.save(Result.create(savedAssessment.id(), null, STUDENT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com"));
        entityManager.flush();

        results.save(Result.create(savedAssessment.id(), UUID.randomUUID(), STUDENT_ID,
                new BigDecimal("12"), ResultStatus.SCORED, "professor@example.com"));

        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }

    @Test
    void databaseRejectsAbsentResultWithScore()
    {
        Assessment savedAssessment = assessments.save(Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, new BigDecimal("20")));
        entityManager.flush();

        // The domain record forbids this, so the entity is written directly
        // to check that the database constraint holds on its own.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entityManager.persist(new ResultEntity(UUID.randomUUID(), savedAssessment.id(), null,
                STUDENT_ID, new BigDecimal("10"), ResultStatus.ABSENT.name(),
                "professor@example.com", now, now));

        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }

    @Test
    void databaseRejectsAssessmentForUnknownCourse()
    {
        assessments.save(Assessment.create(UUID.randomUUID(),
                "Final exam", AssessmentType.EXAM, new BigDecimal("20")));

        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }

    @Test
    void correctionUpdatesScoreAndKeepsEntryAuditFields()
    {
        Assessment savedAssessment = assessments.save(Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, new BigDecimal("20")));
        entityManager.flush();
        Result savedResult = results.save(Result.create(savedAssessment.id(), null, STUDENT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com"));
        entityManager.flush();

        Result corrected = results.save(savedResult.corrected(new BigDecimal("12"),
                ResultStatus.SCORED));
        entityManager.flush();

        Result found = results.findById(corrected.id()).orElseThrow();
        assertEquals(new BigDecimal("12"), found.score());
        assertEquals("professor@example.com", found.enteredBy());
        assertEquals(savedResult.enteredAt(), found.enteredAt());
    }
}
