package com.dess.academic.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.dess.academic.application.StudentGroupService;
import com.dess.academic.domain.StudentGroup;
import com.dess.academic.exception.DuplicateStudentGroupException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import java.util.HashMap;
import java.util.List;
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

/** Tests d'intégration JPA de PostgreSQL pour les groupes d'étudiants. */
@Testcontainers(disabledWithoutDocker = true)
class StudentGroupRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dess_test")
            .withUsername("dess")
            .withPassword("dess");

    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private StudentGroupRepository repository;
    private StudentGroupService service;

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
        entityManagerFactory = Persistence.createEntityManagerFactory("studentGroupTestPU", properties);
    }

    @AfterAll
    static void closeJpa() {
        if (entityManagerFactory != null) entityManagerFactory.close();
    }

    @BeforeEach
    void open() {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        repository = new StudentGroupRepository(entityManager);
        service = new StudentGroupService(repository);
    }

    @AfterEach
    void close() {
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        entityManager.close();
    }

    @Test
    void createsAndReloadsStudentGroup() {
        UUID periodId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        StudentGroup created = service.create(" ta ", " Terminale A ", unitId, periodId);
        entityManager.flush();

        StudentGroup found = repository.findById(created.getId()).orElseThrow();
        assertEquals("TA", found.getCode());
        assertEquals("Terminale A", found.getName());
        assertTrue(found.isActive());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    void rejectsDuplicateCodeInSamePeriod() {
        UUID periodId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        service.create("A1", "Première A", unitId, periodId);

        assertThrows(DuplicateStudentGroupException.class,
                () -> service.create(" a1 ", "Autre groupe", unitId, periodId));
    }

    @Test
    void acceptsSameCodeInAnotherPeriod() {
        UUID unitId = UUID.randomUUID();
        service.create("A1", "Première A", unitId, UUID.randomUUID());

        assertDoesNotThrow(() -> service.create("A1", "Première A",
                unitId, UUID.randomUUID()));
    }

    @Test
    void rejectsBlankCodeOrName() {
        UUID unitId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> service.create("  ", "Terminale A", unitId, periodId));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("TA", "  ", unitId, periodId));
    }

    @Test
    void databaseRejectsDuplicateCodeAndPeriod() {
        UUID unitId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        repository.save(new StudentGroup("A1", "Groupe A", unitId, periodId));
        entityManager.flush();

        repository.save(new StudentGroup("A1", "Groupe identique", unitId, periodId));

        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }

    @Test
    void listsOnlyRequestedPeriodSortedByCode() {
        UUID requestedPeriod = UUID.randomUUID();
        UUID otherPeriod = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        service.create("C", "Groupe C", unitId, requestedPeriod);
        service.create("A", "Groupe A", unitId, requestedPeriod);
        service.create("B", "Groupe B", unitId, otherPeriod);
        entityManager.flush();

        List<StudentGroup> found = service.listByPeriod(requestedPeriod);

        assertEquals(List.of("A", "C"), found.stream().map(StudentGroup::getCode).toList());
        assertTrue(found.stream().noneMatch(group -> group.getAcademicPeriodId().equals(otherPeriod)));
    }
}
