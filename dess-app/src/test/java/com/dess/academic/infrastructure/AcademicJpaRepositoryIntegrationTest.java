package com.dess.academic.infrastructure;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.CreateProgramVersionCommand;
import com.dess.academic.application.spi.PeriodRepository;
import com.dess.academic.application.spi.ProgramRepository;
import com.dess.academic.application.spi.ProgramVersionRepository;
import com.dess.academic.domain.AcademicPeriod;
import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.DuplicatePeriodException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests d'intégration des repositories JPA period/programme/version. */
@Testcontainers(disabledWithoutDocker = true)
class AcademicJpaRepositoryIntegrationTest {
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
    void createsPeriodAndRejectsDuplicate() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            PeriodRepository periods = new JpaPeriodRepository(entityManager);
            AcademicPeriod period = periods.create(new CreatePeriodCommand("2026-2027", 1));

            assertEquals("2026-2027", period.academicYear());
            assertTrue(periods.findAll().stream().anyMatch(p -> p.id().equals(period.id())));
            assertThrows(DuplicatePeriodException.class,
                    () -> periods.create(new CreatePeriodCommand("2026-2027", 1)));

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    @Test
    void createsProgramAndVersionThenActivatesIt() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            JpaProgramRepository programs = new JpaProgramRepository(entityManager);
            JpaProgramVersionRepository versions = new JpaProgramVersionRepository(entityManager);

            AcademicProgram program = programs.create(new CreateProgramCommand("L3-INF", "Licence Informatique"));
            assertEquals(AcademicProgramStatus.ACTIVE, program.status());

            ProgramVersion draft = versions.create(new CreateProgramVersionCommand(
                    program.id(), "2026-L3", LocalDate.of(2026, 9, 1), null));
            assertEquals(ProgramVersionStatus.DRAFT, draft.status());

            ProgramVersion active = versions.updateStatus(draft.id(), ProgramVersionStatus.ACTIVE);
            assertEquals(ProgramVersionStatus.ACTIVE, active.status());

            assertThrows(AcademicException.class,
                    () -> programs.create(new CreateProgramCommand("L3-INF", "Doublon")));
            assertThrows(AcademicException.class,
                    () -> versions.create(new CreateProgramVersionCommand(
                            program.id(), "2026-L3", LocalDate.of(2026, 9, 1), null)));

            entityManager.getTransaction().rollback();
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
            throw exception;
        } finally {
            entityManager.close();
        }
    }
}
