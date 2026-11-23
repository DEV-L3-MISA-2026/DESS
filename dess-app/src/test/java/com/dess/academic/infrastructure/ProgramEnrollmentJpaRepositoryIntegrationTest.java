package com.dess.academic.infrastructure;

import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.academic.exception.AcademicException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Test d'intégration JPA des inscriptions aux versions de programmes. */
@Testcontainers(disabledWithoutDocker = true)
class ProgramEnrollmentJpaRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dess_test")
            .withUsername("dess")
            .withPassword("dess");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndStartJpa() {
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
        entityManagerFactory = Persistence.createEntityManagerFactory("academicTestPU", properties);
    }

    @AfterAll
    static void closeJpa() {
        if (entityManagerFactory != null) entityManagerFactory.close();
    }

    @Test
    void createsActiveEnrollmentAndRejectsSecondActiveEnrollment() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            long studentId = createStudent(entityManager, "student1@dess.mg", "STU-INT-1");

            AcademicProgramEntity program = new AcademicProgramEntity("LICENCE-INT", "Licence intégration", "ACTIVE");
            entityManager.persist(program);
            entityManager.flush();
            ProgramVersionEntity version = new ProgramVersionEntity(program.getId().longValue(), "2026-L3",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30), "ACTIVE");
            entityManager.persist(version);
            entityManager.flush();

            long programVersionId = version.getId();

            JpaProgramEnrollmentRepository repository = new JpaProgramEnrollmentRepository(entityManager);

            ProgramEnrollment first = repository.create(new ProgramEnrollment(null, studentId, programVersionId,
                    LocalDate.of(2026, 9, 1), null, EnrollmentStatus.ACTIVE));
            assertEquals(EnrollmentStatus.ACTIVE, first.status());

            assertThrows(AcademicException.class, () -> repository.create(new ProgramEnrollment(null, studentId, programVersionId,
                    LocalDate.of(2026, 9, 2), null, EnrollmentStatus.ACTIVE)));

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void completesEnrollment() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            long studentId = createStudent(entityManager, "student2@dess.mg", "STU-INT-2");

            AcademicProgramEntity program = new AcademicProgramEntity("MASTER-INT", "Master intégration", "ACTIVE");
            entityManager.persist(program);
            entityManager.flush();
            ProgramVersionEntity version = new ProgramVersionEntity(program.getId().longValue(), "2026-M1",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30), "ACTIVE");
            entityManager.persist(version);
            entityManager.flush();

            JpaProgramEnrollmentRepository repository = new JpaProgramEnrollmentRepository(entityManager);

            ProgramEnrollment active = repository.create(new ProgramEnrollment(null, studentId, version.getId(),
                    LocalDate.of(2026, 9, 1), null, EnrollmentStatus.ACTIVE));
            ProgramEnrollment completed = repository.update(new ProgramEnrollment(active.id(), active.studentId(),
                    active.programVersionId(), active.startDate(), LocalDate.of(2027, 6, 30),
                    EnrollmentStatus.COMPLETED));

            assertEquals(EnrollmentStatus.COMPLETED, completed.status());
            entityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    private long createStudent(EntityManager entityManager, String email, String registrationNumber) {
        Number appUserId = (Number) entityManager.createNativeQuery(
                        "INSERT INTO app_user (email, last_name, first_name, role, active) "
                                + "VALUES (:email, :lastName, :firstName, 'STUDENT', true) RETURNING id")
                .setParameter("email", email)
                .setParameter("lastName", "Test")
                .setParameter("firstName", "Student")
                .getSingleResult();

        entityManager.createNativeQuery(
                        "INSERT INTO student (id, registration_number) VALUES (:id, :registrationNumber)")
                .setParameter("id", appUserId.longValue())
                .setParameter("registrationNumber", registrationNumber)
                .executeUpdate();

        return appUserId.longValue();
    }
}
