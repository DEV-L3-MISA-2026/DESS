package com.dess.academic.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.dess.academic.domain.Course;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Tests d'intégration du repository JPA des cours avec PostgreSQL réel. */
@Testcontainers(disabledWithoutDocker = true)
class CourseRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dess_test")
            .withUsername("dess")
            .withPassword("dess");

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private JpaCourseRepository repository;

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
        entityManagerFactory = Persistence.createEntityManagerFactory("courseTestPU", properties);
    }

    @AfterAll
    static void closeJpa() {
        if (entityManagerFactory != null) entityManagerFactory.close();
    }

    @BeforeEach
    void open() {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        repository = new JpaCourseRepository(entityManager);
    }

    @AfterEach
    void close() {
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        entityManager.close();
    }

    @Test
    void savesAndFindsByCode() {
        repository.save(Course.create("math101", "Mathématiques"));
        entityManager.flush();

        Course found = repository.findByCode("MATH101").orElseThrow();

        assertEquals("Mathématiques", found.title());
        assertTrue(found.active());
        assertNotNull(found.createdAt());
        assertTrue(repository.existsByCode("MATH101"));
        assertFalse(repository.existsByCode("MATH102"));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(found.id()).isPresent());
    }

    @Test
    void databaseRejectsDuplicateCode() {
        repository.save(Course.create("PHY1", "Physique"));
        entityManager.flush();

        repository.save(Course.create("PHY1", "Autre physique"));

        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }

    @Test
    void blankCodeIsRejectedByDomain() {
        assertThrows(IllegalArgumentException.class, () -> Course.create("  ", "X"));
        assertThrows(IllegalArgumentException.class, () -> Course.create("X", "  "));
    }
}
