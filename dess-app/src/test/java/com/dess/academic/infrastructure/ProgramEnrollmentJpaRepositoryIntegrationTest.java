package com.dess.academic.infrastructure;

import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.EnrollmentConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void rejectsOnlyOneOfTwoConcurrentActiveEnrollments() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int iteration = 0; iteration < 20; iteration++) {
                EntityManager setupEntityManager = entityManagerFactory.createEntityManager();
                long studentId;
                long programVersionId;
                setupEntityManager.getTransaction().begin();
                try {
                    String suffix = "-conc-" + iteration;
                    studentId = createStudent(setupEntityManager,
                            "student" + suffix + "@dess.mg", "STU" + suffix);
                    AcademicProgramEntity program = new AcademicProgramEntity(
                            "PROG" + suffix, "Programme concurrence", "ACTIVE");
                    setupEntityManager.persist(program);
                    setupEntityManager.flush();
                    ProgramVersionEntity version = new ProgramVersionEntity(program.getId().longValue(),
                            "V" + suffix, LocalDate.of(2026, 9, 1),
                            LocalDate.of(2027, 6, 30), "ACTIVE");
                    setupEntityManager.persist(version);
                    setupEntityManager.flush();
                    programVersionId = version.getId();
                    setupEntityManager.getTransaction().commit();
                } catch (RuntimeException exception) {
                    if (setupEntityManager.getTransaction().isActive()) {
                        setupEntityManager.getTransaction().rollback();
                    }
                    throw exception;
                } finally {
                    setupEntityManager.close();
                }

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                Callable<Boolean> attempt = () -> {
                    EntityManager entityManager = entityManagerFactory.createEntityManager();
                    try {
                        ready.countDown();
                        assertTrue(start.await(30, TimeUnit.SECONDS),
                                "Les deux tentatives d’inscription doivent démarrer ensemble");
                        entityManager.getTransaction().begin();
                        JpaProgramEnrollmentRepository repository =
                                new JpaProgramEnrollmentRepository(entityManager);
                        repository.create(new ProgramEnrollment(null, studentId, programVersionId,
                                LocalDate.of(2026, 9, 1), null, EnrollmentStatus.ACTIVE));
                        entityManager.getTransaction().commit();
                        return true;
                    } catch (EnrollmentConflictException exception) {
                        if (entityManager.getTransaction().isActive()) {
                            entityManager.getTransaction().rollback();
                        }
                        assertTrue(exception.getCause() != null,
                                "EnrollmentConflictException doit conserver la violation d’index comme cause");
                        Throwable root = rootCause(exception);
                        assertTrue(root.getMessage() != null
                                && (root.getMessage().toLowerCase().contains("uq_program_enrollment_active_student_version")
                                || root.getMessage().contains("23505")),
                                "La cause doit identifier la violation de l’index unique");
                        return false;
                    } catch (RuntimeException exception) {
                        if (entityManager.getTransaction().isActive()) {
                            entityManager.getTransaction().rollback();
                        }
                        throw exception;
                    } finally {
                        entityManager.close();
                    }
                };

                Future<Boolean> first = executor.submit(attempt);
                Future<Boolean> second = executor.submit(attempt);
                assertTrue(ready.await(30, TimeUnit.SECONDS),
                        "Les deux threads doivent être prêts avant le départ");
                start.countDown();

                int successCount = (first.get(30, TimeUnit.SECONDS) ? 1 : 0)
                        + (second.get(30, TimeUnit.SECONDS) ? 1 : 0);
                assertEquals(1, successCount);

                EntityManager verifyEntityManager = entityManagerFactory.createEntityManager();
                try {
                    Number activeCount = (Number) verifyEntityManager.createNativeQuery(
                                    "SELECT COUNT(*) FROM program_enrollment "
                                            + "WHERE student_id = :studentId AND program_version_id = :versionId "
                                            + "AND status = 'ACTIVE'")
                            .setParameter("studentId", studentId)
                            .setParameter("versionId", programVersionId)
                            .getSingleResult();
                    assertEquals(1, activeCount.intValue());
                } finally {
                    verifyEntityManager.close();
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    @Test
    void detectsConcurrentUpdateWithOptimisticLocking() throws Exception {
        EntityManager setupEntityManager = entityManagerFactory.createEntityManager();
        long enrollmentId;
        setupEntityManager.getTransaction().begin();
        try {
            long studentId = createStudent(setupEntityManager,
                    "student-optlock@dess.mg", "STU-OPTLOCK");
            AcademicProgramEntity program = new AcademicProgramEntity(
                    "PROG-OPTLOCK", "Programme verrou optimiste", "ACTIVE");
            setupEntityManager.persist(program);
            setupEntityManager.flush();
            ProgramVersionEntity version = new ProgramVersionEntity(program.getId().longValue(),
                    "V-OPTLOCK", LocalDate.of(2026, 9, 1),
                    LocalDate.of(2027, 6, 30), "ACTIVE");
            setupEntityManager.persist(version);
            setupEntityManager.flush();

            JpaProgramEnrollmentRepository repository = new JpaProgramEnrollmentRepository(setupEntityManager);
            ProgramEnrollment active = repository.create(new ProgramEnrollment(null, studentId, version.getId(),
                    LocalDate.of(2026, 9, 1), null, EnrollmentStatus.ACTIVE));
            enrollmentId = active.id();
            setupEntityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (setupEntityManager.getTransaction().isActive()) {
                setupEntityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            setupEntityManager.close();
        }

        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<EnrollmentStatus> completeAttempt = () -> {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                entityManager.getTransaction().begin();
                JpaProgramEnrollmentRepository repository = new JpaProgramEnrollmentRepository(entityManager);
                ProgramEnrollment active = repository.findById(enrollmentId).orElseThrow();
                loaded.countDown();
                assertTrue(start.await(30, TimeUnit.SECONDS));
                ProgramEnrollment completed = repository.update(new ProgramEnrollment(active.id(),
                        active.studentId(), active.programVersionId(), active.startDate(),
                        LocalDate.of(2027, 6, 30), EnrollmentStatus.COMPLETED));
                entityManager.getTransaction().commit();
                return completed.status();
            } catch (RuntimeException exception) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw exception;
            } finally {
                entityManager.close();
            }
        };
        Callable<EnrollmentStatus> withdrawAttempt = () -> {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                entityManager.getTransaction().begin();
                JpaProgramEnrollmentRepository repository = new JpaProgramEnrollmentRepository(entityManager);
                ProgramEnrollment active = repository.findById(enrollmentId).orElseThrow();
                loaded.countDown();
                assertTrue(start.await(30, TimeUnit.SECONDS));
                ProgramEnrollment withdrawn = repository.update(new ProgramEnrollment(active.id(),
                        active.studentId(), active.programVersionId(), active.startDate(),
                        LocalDate.of(2027, 6, 30), EnrollmentStatus.WITHDRAWN));
                entityManager.getTransaction().commit();
                return withdrawn.status();
            } catch (RuntimeException exception) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw exception;
            } finally {
                entityManager.close();
            }
        };

        try {
            Future<EnrollmentStatus> first = executor.submit(completeAttempt);
            Future<EnrollmentStatus> second = executor.submit(withdrawAttempt);
            assertTrue(loaded.await(30, TimeUnit.SECONDS));
            start.countDown();

            List<Throwable> failures = new ArrayList<>();
            List<EnrollmentStatus> successes = new ArrayList<>();
            for (Future<EnrollmentStatus> future : List.of(first, second)) {
                try {
                    successes.add(future.get(30, TimeUnit.SECONDS));
                } catch (java.util.concurrent.ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }

            assertEquals(1, successes.size());
            assertEquals(1, failures.size());
            assertTrue(isOptimisticLockFailure(failures.get(0)),
                    "Le perdant doit échouer sur OptimisticLockException : " + failures.get(0));

            EntityManager verifyEntityManager = entityManagerFactory.createEntityManager();
            try {
                Object[] row = (Object[]) verifyEntityManager.createNativeQuery(
                                "SELECT status, version FROM program_enrollment WHERE id = :id")
                        .setParameter("id", enrollmentId)
                        .getSingleResult();
                EnrollmentStatus finalStatus = EnrollmentStatus.valueOf(row[0].toString());
                assertEquals(successes.get(0), finalStatus);
                assertEquals(1, ((Number) row[1]).longValue());
            } finally {
                verifyEntityManager.close();
            }
        } finally {
            executor.shutdownNow();
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

    private Throwable rootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause;
    }

    private boolean isOptimisticLockFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof OptimisticLockException) return true;
            current = current.getCause();
        }
        return exception.getMessage() != null
                && exception.getMessage().toLowerCase().contains("optimistic");
    }
}
