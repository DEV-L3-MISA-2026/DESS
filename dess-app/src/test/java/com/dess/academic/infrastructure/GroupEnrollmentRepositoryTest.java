package com.dess.academic.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.dess.academic.application.GroupEnrollmentService;
import com.dess.academic.application.impl.GroupEnrollmentServiceImpl;
import com.dess.academic.domain.GroupEnrollment;
import com.dess.academic.exception.DuplicateEnrollmentException;
import com.dess.academic.exception.EnrollmentNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Tests d'intégration PostgreSQL des inscriptions de groupes. */
@Testcontainers(disabledWithoutDocker = true)
class GroupEnrollmentRepositoryTest
{
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dess_test")
            .withUsername("dess")
            .withPassword("dess");

    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private GroupEnrollmentRepository repository;
    private GroupEnrollmentService service;

    @BeforeAll
    static void migrateAndStartJpa()
    {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:../db-migration/src/main/resources/db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();

        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", POSTGRES.getUsername());
        properties.put("jakarta.persistence.jdbc.password", POSTGRES.getPassword());
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        entityManagerFactory = Persistence.createEntityManagerFactory("groupEnrollmentTestPU", properties);
    }

    @AfterAll
    static void closeJpa()
    {
        if (entityManagerFactory != null) entityManagerFactory.close();
    }

    @BeforeEach
    void open()
    {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        repository = new GroupEnrollmentRepository(entityManager);
        service = new GroupEnrollmentServiceImpl(repository);
    }

    @AfterEach
    void close()
    {
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        entityManager.close();
    }

    @Test
    void enrollsAndReadsBackEnrollment()
    {
        UUID studentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();

        GroupEnrollment created = service.enroll(studentId, groupId, periodId);
        entityManager.flush();

        GroupEnrollment found = repository.findById(created.id()).orElseThrow();
        assertEquals(studentId, found.studentId());
        assertEquals(groupId, found.groupId());
        assertEquals(periodId, found.academicPeriodId());
        assertNotNull(found.enrolledAt());
    }

    @Test
    void rejectsDuplicateEnrollment()
    {
        UUID studentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();

        service.enroll(studentId, groupId, periodId);

        DuplicateEnrollmentException error = assertThrows(DuplicateEnrollmentException.class,
                () -> service.enroll(studentId, groupId, periodId));
        assertTrue(error.getMessage().contains(studentId.toString()));
        assertTrue(error.getMessage().contains(groupId.toString()));
        assertTrue(error.getMessage().contains(periodId.toString()));
    }

    @Test
    void acceptsDifferentGroupOrPeriod()
    {
        UUID studentId = UUID.randomUUID();
        UUID firstGroup = UUID.randomUUID();
        UUID secondGroup = UUID.randomUUID();
        UUID firstPeriod = UUID.randomUUID();
        UUID secondPeriod = UUID.randomUUID();

        assertDoesNotThrow(() -> service.enroll(studentId, firstGroup, firstPeriod));
        assertDoesNotThrow(() -> service.enroll(studentId, secondGroup, firstPeriod));
        assertDoesNotThrow(() -> service.enroll(studentId, firstGroup, secondPeriod));
    }

    @Test
    void removeDeletesEnrollment()
    {
        UUID enrollmentId = service.enroll(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).id();

        service.remove(enrollmentId);

        assertTrue(repository.findById(enrollmentId).isEmpty());
    }

    @Test
    void removingMissingEnrollmentFailsClearly()
    {
        UUID missingId = UUID.randomUUID();
        assertThrows(EnrollmentNotFoundException.class, () -> service.remove(missingId));
    }

    @Test
    void databaseRejectsDirectDuplicateInsert()
    {
        UUID studentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();

        repository.save(GroupEnrollment.create(studentId, groupId, periodId));
        entityManager.flush();

        repository.save(GroupEnrollment.create(studentId, groupId, periodId));
        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }
}
