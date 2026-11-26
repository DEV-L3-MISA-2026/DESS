package com.dess.people.infrastructure;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.UpdateStudentCommand;
import com.dess.people.exception.DuplicateEmailException;
import com.dess.people.exception.DuplicateRegistrationNumberException;
import com.dess.people.exception.PersonNotFoundException;
import com.dess.people.exception.StudentValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Tests d’intégration des repositories JPA people avec PostgreSQL réel. */
@Testcontainers(disabledWithoutDocker = true)
class PeopleJpaRepositoryIntegrationTest {
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
        entityManagerFactory = Persistence.createEntityManagerFactory("peopleTestPU", properties);
    }

    @AfterAll
    static void closeJpa() {
        if (entityManagerFactory != null) entityManagerFactory.close();
    }

    @Test
    void createsFindsAndListsPeople() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaPersonRepository people = new JpaPersonRepository(entityManager);
            JpaStudentRepository students = new JpaStudentRepository(entityManager);
            JpaTeacherRepository teachers = new JpaTeacherRepository(entityManager);

            var student = students.create(new CreateStudentCommand("integration.student@dess.mg", "Rakoto", "Jean", "INT-STUDENT-1"));
            var teacher = teachers.create(new CreateTeacherCommand("integration.teacher@dess.mg", "Rabe", "Paul", "Professeur"));

            assertEquals("integration.student@dess.mg", people.findById(student.personId()).orElseThrow().email());
            assertEquals("integration.student@dess.mg", people.findByEmail("integration.student@dess.mg").orElseThrow().email());
            assertTrue(students.exists(student.personId()));
            assertEquals(1, students.findAll().stream().filter(s -> s.personId() == student.personId()).count());
            assertEquals("Professeur", teacher.academicRank());

            entityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void rejectsDuplicateEmailAndRegistrationNumber() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaStudentRepository students = new JpaStudentRepository(entityManager);
            students.create(new CreateStudentCommand("unique.student@dess.mg", "Rakoto", "Jean", "INT-STUDENT-2"));
            entityManager.flush();

            assertThrows(DuplicateEmailException.class,
                    () -> students.create(new CreateStudentCommand("unique.student@dess.mg", "Rabe", "Paul", "INT-STUDENT-3")));
            assertThrows(DuplicateRegistrationNumberException.class,
                    () -> students.create(new CreateStudentCommand("other.student@dess.mg", "Rabe", "Paul", "INT-STUDENT-2")));

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void rejectsCaseDifferentEmailAtDatabaseAndPreservesOriginalCase() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            entityManager.createNativeQuery(
                            "INSERT INTO app_user (email, last_name, first_name, role, active) "
                                    + "VALUES ('Ada@x', 'Rakoto', 'Jean', 'STUDENT', true)")
                    .executeUpdate();

            String storedEmail = entityManager.createNativeQuery(
                            "SELECT email FROM app_user WHERE lower(email) = 'ada@x'")
                    .getSingleResult().toString();
            assertEquals("Ada@x", storedEmail);

            assertThrows(PersistenceException.class, () -> entityManager.createNativeQuery(
                            "INSERT INTO app_user (email, last_name, first_name, role, active) "
                                    + "VALUES ('ada@x', 'Rabe', 'Paul', 'STUDENT', true)")
                    .executeUpdate());

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void updatesStudentData() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaPersonRepository people = new JpaPersonRepository(entityManager);
            JpaStudentRepository students = new JpaStudentRepository(entityManager);
            var student = students.create(new CreateStudentCommand("update.student@dess.mg", "Rakoto", "Jean", "INT-STUDENT-4"));

            students.update(student.personId(),
                    new UpdateStudentCommand("update.student@dess.mg", "Name", "Updated"));

            var updatedStudent = people.findById(student.personId()).orElseThrow();
            assertEquals("Updated", updatedStudent.firstName());
            assertEquals("Name", updatedStudent.lastName());

            entityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void deactivatesStudent() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaPersonRepository people = new JpaPersonRepository(entityManager);
            JpaStudentRepository students = new JpaStudentRepository(entityManager);
            var student = students.create(new CreateStudentCommand("deactivate.student@dess.mg", "Rakoto", "Jean", "INT-STUDENT-5"));

            students.deactivate(student.personId());
            students.deactivate(student.personId());

            var deactivatedStudent = people.findById(student.personId()).orElseThrow();
            assertFalse(deactivatedStudent.active());

            entityManager.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void rejectsUpdateForNonExistentStudent() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaStudentRepository students = new JpaStudentRepository(entityManager);
            var nonExistentId = 999L;

            assertThrows(PersonNotFoundException.class,
                    () -> students.update(nonExistentId,
                            new UpdateStudentCommand("email@dess.mg", "First", "Last")));
            assertThrows(PersonNotFoundException.class,
                    () -> students.deactivate(nonExistentId));

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void updatesAndDeactivatesStudentWithoutRemovingHistory() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaPersonRepository people = new JpaPersonRepository(entityManager);
            JpaStudentRepository students = new JpaStudentRepository(entityManager);

            var student = students.create(new CreateStudentCommand(
                    "history.student@dess.mg", "Rakoto", "Jean", "INT-HISTORY-1"));

            entityManager.createNativeQuery(
                            "INSERT INTO academic_program (code, name, status) VALUES ('HIST', 'History', 'ACTIVE')")
                    .executeUpdate();
            Long programId = ((Number) entityManager.createNativeQuery(
                            "SELECT id FROM academic_program WHERE code = 'HIST'")
                    .getSingleResult()).longValue();
            entityManager.createNativeQuery(
                            "INSERT INTO program_version (program_id, code, effective_from, status) "
                                    + "VALUES (:programId, 'HIST-V1', DATE '2026-01-01', 'ACTIVE')")
                    .setParameter("programId", programId)
                    .executeUpdate();
            Long versionId = ((Number) entityManager.createNativeQuery(
                            "SELECT id FROM program_version WHERE code = 'HIST-V1'")
                    .getSingleResult()).longValue();
            entityManager.createNativeQuery(
                            "INSERT INTO program_enrollment "
                                    + "(student_id, program_version_id, start_date, status) "
                                    + "VALUES (:studentId, :versionId, DATE '2026-01-15', 'ACTIVE')")
                    .setParameter("studentId", student.personId())
                    .setParameter("versionId", versionId)
                    .executeUpdate();

            var updated = students.update(student.personId(),
                    new UpdateStudentCommand("History.Student@Dess.Mg", "Rabe", "Paul"));
            assertEquals("History.Student@Dess.Mg", updated.email());

            students.deactivate(student.personId());
            students.deactivate(student.personId());

            assertEquals(0, students.findAll().stream()
                    .filter(profile -> profile.personId() == student.personId()).count());
            assertEquals(1, students.findAllIncludingInactive().stream()
                    .filter(profile -> profile.personId() == student.personId()).count());
            assertTrue(people.findById(student.personId()).orElseThrow().active() == false);

            Number enrollmentCount = (Number) entityManager.createNativeQuery(
                            "SELECT count(*) FROM program_enrollment WHERE student_id = :studentId")
                    .setParameter("studentId", student.personId())
                    .getSingleResult();
            assertEquals(1, enrollmentCount.intValue());

            Number studentCount = (Number) entityManager.createNativeQuery(
                            "SELECT count(*) FROM student WHERE id = :studentId")
                    .setParameter("studentId", student.personId())
                    .getSingleResult();
            assertEquals(1, studentCount.intValue());

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }
}
