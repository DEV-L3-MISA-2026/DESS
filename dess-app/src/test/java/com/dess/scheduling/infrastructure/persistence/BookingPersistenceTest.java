package com.dess.scheduling.infrastructure.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.impl.BookingServiceImpl;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.Room;
import com.dess.scheduling.infrastructure.persistence.entity.BookingEntity;

/**
 * Integration tests against a real PostgreSQL 17 (Testcontainers).
 *
 * Only the scheduling migration is applied: V1 already creates an
 * incompatible room table (bigint ids) and which migration owns the room
 * table is still an open team decision (PR #6, point 3). These tests verify
 * what only the real database can guarantee: the exclusion constraint, the
 * ON DELETE CASCADE and the database-filled created_at.
 */
@Testcontainers(disabledWithoutDocker = true)
class BookingPersistenceTest
{
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-25T09:00:00+03:00");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-11-25T11:00:00+03:00");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUpDatabase() throws Exception
    {
        applySchedulingMigration();
        Map<String, Object> overrides = Map.of(
                "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                "jakarta.persistence.jdbc.password", POSTGRES.getPassword(),
                "jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        emf = Persistence.createEntityManagerFactory("dessSchedulingTestPU", overrides);
    }

    @BeforeEach
    void cleanTables() throws Exception
    {
        try (Connection connection = jdbc(); Statement statement = connection.createStatement())
        {
            statement.execute("TRUNCATE booking_slot, booking, room");
        }
    }

    @Test
    void createdAtIsFilledByTheDatabase() throws Exception
    {
        UUID roomId = createRoom(30);

        Booking created = inTransaction(service ->
                service.create("SESSION", UUID.randomUUID(), START, END,
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));

        assertNotNull(created.createdAt());
        try (Connection connection = jdbc(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT created_at FROM booking WHERE id = '" + created.id() + "'"))
        {
            assertTrue(rs.next());
            assertNotNull(rs.getObject(1));
        }
    }

    @Test
    void databaseRejectsOverlappingActiveSlot() throws Exception
    {
        UUID roomId = createRoom(30);
        Booking created = inTransaction(service ->
                service.create("SESSION", UUID.randomUUID(), START, END,
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));

        EntityManager em = emf.createEntityManager();
        try
        {
            em.getTransaction().begin();
            JpaBookingSlotRepository slots = new JpaBookingSlotRepository(em);
            slots.save(BookingSlot.create(created.id(), "ROOM", roomId,
                    START.plusMinutes(30), END.plusMinutes(30)));
            assertThrows(PersistenceException.class, () -> em.getTransaction().commit());
        }
        finally
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    @Test
    void cancelledBookingFreesItsTimeRange() throws Exception
    {
        UUID roomId = createRoom(30);
        Booking created = inTransaction(service ->
                service.create("SESSION", UUID.randomUUID(), START, END,
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));

        inTransaction(service ->
        {
            service.cancel(created.id());
            return null;
        });

        Booking again = inTransaction(service ->
                service.create("SESSION", UUID.randomUUID(), START, END,
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));

        assertNotNull(again);
    }

    @Test
    void deletingBookingDeletesItsSlots() throws Exception
    {
        UUID roomId = createRoom(30);
        Booking created = inTransaction(service ->
                service.create("SESSION", UUID.randomUUID(), START, END,
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));
        assertEquals(1, countRows("booking_slot"));

        EntityManager em = emf.createEntityManager();
        try
        {
            em.getTransaction().begin();
            BookingEntity entity = em.find(BookingEntity.class, created.id());
            em.remove(entity);
            em.getTransaction().commit();
        }
        finally
        {
            em.close();
        }

        assertEquals(0, countRows("booking_slot"));
        assertEquals(0, countRows("booking"));
    }

    @Test
    void concurrentBookingsForSameRoomOnlyOneWins() throws Exception
    {
        UUID roomId = createRoom(30);
        int attempts = 2;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        try
        {
            Future<?>[] futures = new Future<?>[attempts];
            for (int i = 0; i < attempts; i++)
            {
                futures[i] = pool.submit(() ->
                {
                    ready.countDown();
                    go.await(10, TimeUnit.SECONDS);
                    try
                    {
                        inTransaction(service ->
                        {
                            service.create("SESSION", UUID.randomUUID(), START, END,
                                    List.of(new BookingService.ResourceRequest("ROOM", roomId, 10)));
                            return null;
                        });
                        successes.incrementAndGet();
                    }
                    catch (Exception refused)
                    {
                        // The loser is refused, either by the service check or
                        // by the database constraint. Both are acceptable.
                    }
                    return null;
                });
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS));
            go.countDown();
            for (Future<?> future : futures)
            {
                future.get(60, TimeUnit.SECONDS);
            }
        }
        finally
        {
            pool.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(1, countRows("booking"));
    }

    private UUID createRoom(int capacity) throws Exception
    {
        UUID roomId = UUID.randomUUID();
        EntityManager em = emf.createEntityManager();
        try
        {
            em.getTransaction().begin();
            new JpaRoomRepository(em).save(new Room(roomId, "A101", "Salle A101", capacity));
            em.getTransaction().commit();
        }
        finally
        {
            em.close();
        }
        return roomId;
    }

    private <T> T inTransaction(ServiceCall<T> call) throws Exception
    {
        EntityManager em = emf.createEntityManager();
        try
        {
            em.getTransaction().begin();
            BookingService service = new BookingServiceImpl(
                    new JpaBookingRepository(em),
                    new JpaBookingSlotRepository(em),
                    new JpaRoomRepository(em));
            T result = call.run(service);
            em.getTransaction().commit();
            return result;
        }
        catch (RuntimeException e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            throw e;
        }
        finally
        {
            em.close();
        }
    }

    private int countRows(String table) throws Exception
    {
        try (Connection connection = jdbc(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM " + table))
        {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Connection jdbc() throws Exception
    {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void applySchedulingMigration() throws Exception
{
    Path migration = findMigrationFile();
    String sql = Files.readString(migration);
    String withoutComments = sql.replaceAll("(?m)^\\s*--.*$", "");
    try (Connection connection = jdbc(); Statement statement = connection.createStatement())
    {
        for (String part : withoutComments.split(";"))
        {
            String cleaned = part.trim();
            if (!cleaned.isEmpty())
            {
                statement.execute(cleaned);
            }
        }
    }
}


    private static Path findMigrationFile()
    {
        String relative = "db-migration/src/main/resources/db/migration/"
                + "V20261117_0900__scheduling_create_room_and_booking.sql";
        Path fromRoot = Path.of(relative);
        if (Files.exists(fromRoot))
        {
            return fromRoot;
        }
        Path fromModule = Path.of("..").resolve(relative);
        if (Files.exists(fromModule))
        {
            return fromModule;
        }
        throw new IllegalStateException("scheduling migration not found from " + Path.of("").toAbsolutePath());
    }

    @FunctionalInterface
    private interface ServiceCall<T>
    {
        T run(BookingService service);
    }
}
