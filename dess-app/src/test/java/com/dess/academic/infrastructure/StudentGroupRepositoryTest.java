package com.dess.academic.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dess.academic.application.StudentGroupService;
import com.dess.academic.domain.StudentGroup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
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

@Testcontainers
class StudentGroupRepositoryTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    static EntityManagerFactory emf;
    EntityManager em;
    StudentGroupRepository repo;
    StudentGroupService service;

    @BeforeAll
    static void setUp() {
        Flyway.configure()
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        emf = Persistence.createEntityManagerFactory("dess-test", Map.of(
                "jakarta.persistence.jdbc.url", pg.getJdbcUrl(),
                "jakarta.persistence.jdbc.user", pg.getUsername(),
                "jakarta.persistence.jdbc.password", pg.getPassword()));
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @BeforeEach
    void open() {
        em = emf.createEntityManager();
        repo = new StudentGroupRepository(em);
        service = new StudentGroupService(repo);
        em.getTransaction().begin();
    }

    @AfterEach
    void close() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        em.close();
    }

    @Test
    void creates_then_reads_by_id() {
        UUID owningUnitId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        StudentGroup created = service.create(" ta-a ", "Terminale A", owningUnitId, periodId);
        em.flush();

        StudentGroup found = repo.findById(created.getId()).orElseThrow();
        assertEquals("TA-A", found.getCode());
        assertEquals("Terminale A", found.getName());
        assertEquals(owningUnitId, found.getOwningUnitId());
        assertEquals(periodId, found.getAcademicPeriodId());
        assertTrue(found.isActive());
    }

    @Test
    void rejects_duplicate_code_in_same_period() {
        UUID periodId = UUID.randomUUID();
        service.create("TA-A", "Terminale A", UUID.randomUUID(), periodId);
        em.flush();

        assertThrows(
                StudentGroupService.DuplicateStudentGroupException.class,
                () -> service.create("ta-a", "Terminale A bis", UUID.randomUUID(), periodId));
    }

    @Test
    void accepts_same_code_in_another_period() {
        StudentGroup first = service.create("TA-A", "Terminale A 2026", UUID.randomUUID(), UUID.randomUUID());
        StudentGroup second = service.create("TA-A", "Terminale A 2027", UUID.randomUUID(), UUID.randomUUID());
        em.flush();

        assertTrue(repo.findById(first.getId()).isPresent());
        assertTrue(repo.findById(second.getId()).isPresent());
    }

    @Test
    void rejects_blank_code_or_name() {
        UUID periodId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> service.create("  ", "Terminale A", UUID.randomUUID(), periodId));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("TA-A", "  ", UUID.randomUUID(), periodId));
    }

    @Test
    void database_rejects_duplicate_code_and_period() {
        UUID periodId = UUID.randomUUID();
        repo.save(new StudentGroup("TA-A", "Terminale A", UUID.randomUUID(), periodId));
        em.flush();
        repo.save(new StudentGroup("TA-A", "Autre Terminale A", UUID.randomUUID(), periodId));

        assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    void findByPeriod_filters_and_sorts_by_code() {
        UUID periodId = UUID.randomUUID();
        UUID otherPeriodId = UUID.randomUUID();

        repo.save(new StudentGroup("T-C", "Terminale C", UUID.randomUUID(), periodId));
        repo.save(new StudentGroup("T-A", "Terminale A", UUID.randomUUID(), periodId));
        repo.save(new StudentGroup("T-B", "Terminale B", UUID.randomUUID(), otherPeriodId));
        em.flush();

        var found = repo.findByPeriod(periodId);
        assertEquals(2, found.size());
        assertEquals("T-A", found.get(0).getCode());
        assertEquals("T-C", found.get(1).getCode());
    }
}
