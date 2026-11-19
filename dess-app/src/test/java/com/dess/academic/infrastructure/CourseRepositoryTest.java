package com.dess.academic.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.dess.academic.domain.Course;
import jakarta.persistence.*;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CourseRepositoryTest 
{

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    static EntityManagerFactory emf;
    EntityManager em;
    CourseRepository repo;

    @BeforeAll
    static void setUp() 
    {
        Flyway.configure()
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").load().migrate();
        emf = Persistence.createEntityManagerFactory("dess-test", Map.of(
                "jakarta.persistence.jdbc.url", pg.getJdbcUrl(),
                "jakarta.persistence.jdbc.user", pg.getUsername(),
                "jakarta.persistence.jdbc.password", pg.getPassword()));
    }

    @AfterAll
    static void tearDown() 
    { 
        emf.close(); 
    }

    @BeforeEach
    void open() 
    { 
        em = emf.createEntityManager(); repo = new CourseRepository(em); em.getTransaction().begin(); 
    }

    @AfterEach
    void close() 
    {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        em.close();
    }

    @Test
    void saves_and_finds_by_code() 
    {
        repo.save(new Course("math101", "Mathématiques"));
        em.flush();
        Course found = repo.findByCode("MATH101").orElseThrow();
        assertEquals("Mathématiques", found.getTitle());
        assertTrue(found.isActive());
    }

    @Test
    void database_rejects_duplicate_code() 
    {
        repo.save(new Course("PHY1", "Physique"));
        em.flush();
        repo.save(new Course("PHY1", "Autre physique"));
        assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    void blank_code_is_rejected_by_domain() 
    {
        assertThrows(IllegalArgumentException.class, () -> new Course("  ", "X"));
    }
}