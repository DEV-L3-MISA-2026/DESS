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

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Tests d'intégration JPA PostgreSQL pour les groupes d'étudiants. */
@Testcontainers(disabledWithoutDocker = true)
class StudentGroupRepositoryTest
{
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
        entityManagerFactory = Persistence.createEntityManagerFactory("studentGroupTestPU", properties);
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
        repository = new StudentGroupRepository(entityManager);
        service = new StudentGroupService(repository);
    }

    @AfterEach
    void close()
    {
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        entityManager.close();
    }

    private long createPeriod()
    {
        PeriodEntity period = new PeriodEntity("2026-2027", 1);
        entityManager.persist(period);
        entityManager.flush();
        return period.getId();
    }

    @Test
    void createsAndReloadsStudentGroup()
    {
        long periodId = createPeriod();
        StudentGroup created = service.create(" ta ", " Terminale A ", "UNIT-SCI", periodId);
        entityManager.flush();

        StudentGroup found = repository.findById(created.id()).orElseThrow();
        assertEquals("TA", found.code());
        assertEquals("Terminale A", found.name());
        assertEquals("UNIT-SCI", found.owningUnitId());
        assertTrue(found.active());
        assertNotNull(found.createdAt());
    }

    @Test
    void rejectsDuplicateCodeInSamePeriod()
    {
        long periodId = createPeriod();
        service.create("A1", "Première A", "UNIT-SCI", periodId);

        assertThrows(DuplicateStudentGroupException.class,
                () -> service.create(" a1 ", "Autre groupe", "UNIT-SCI", periodId));
    }

    @Test
    void acceptsSameCodeInAnotherPeriod()
    {
        long periodOne = createPeriod();
        PeriodEntity periodTwo = new PeriodEntity("2026-2027", 2);
        entityManager.persist(periodTwo);
        entityManager.flush();

        service.create("A1", "Première A", "UNIT-SCI", periodOne);
        assertDoesNotThrow(() -> service.create("A1", "Première A", "UNIT-SCI", periodTwo.getId()));
    }

    @Test
    void rejectsBlankCodeOrName()
    {
        long periodId = createPeriod();

        assertThrows(IllegalArgumentException.class,
                () -> service.create("  ", "Terminale A", "UNIT-SCI", periodId));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("TA", "  ", "UNIT-SCI", periodId));
    }

    @Test
    void rejectsMissingOwningUnit()
    {
        long periodId = createPeriod();
        StudentGroupService validatingService = new StudentGroupService(repository, null, null,
                unitId -> false);

        assertThrows(IllegalArgumentException.class,
                () -> validatingService.create("TA", "Terminale A", "UNIT-MISSING", periodId));
    }

    @Test
    void databaseRejectsDuplicateCodeAndPeriod()
    {
        long periodId = createPeriod();
        repository.save(new StudentGroup("A1", "Groupe A", "UNIT-SCI", periodId));
        entityManager.flush();

        repository.save(new StudentGroup("A1", "Groupe identique", "UNIT-SCI", periodId));
        assertThrows(PersistenceException.class, () -> entityManager.flush());
    }

    @Test
    void listsOnlyRequestedPeriodSortedByCode()
    {
        long periodOne = createPeriod();
        PeriodEntity periodTwo = new PeriodEntity("2026-2027", 2);
        entityManager.persist(periodTwo);
        entityManager.flush();

        service.create("C", "Groupe C", "UNIT-SCI", periodOne);
        service.create("A", "Groupe A", "UNIT-SCI", periodOne);
        service.create("B", "Groupe B", "UNIT-SCI", periodTwo.getId());
        entityManager.flush();

        List<StudentGroup> found = service.listByPeriod(periodOne);
        assertEquals(List.of("A", "C"), found.stream().map(StudentGroup::code).toList());
        assertTrue(found.stream().noneMatch(group -> group.academicPeriodId() == periodTwo.getId()));
    }
}
