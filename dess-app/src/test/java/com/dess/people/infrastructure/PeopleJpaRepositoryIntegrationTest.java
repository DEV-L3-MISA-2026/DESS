package com.dess.people.infrastructure;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.exception.DuplicateEmailException;
import com.dess.people.exception.DuplicateRegistrationNumberException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests d’intégration des repositories JPA people avec PostgreSQL réel. */
@Testcontainers
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
}
