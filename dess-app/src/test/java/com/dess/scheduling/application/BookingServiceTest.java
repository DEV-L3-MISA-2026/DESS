package com.dess.scheduling.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dess.scheduling.application.impl.BookingServiceImpl;
import com.dess.scheduling.application.spi.BookingRepository;
import com.dess.scheduling.application.spi.BookingSlotRepository;
import com.dess.scheduling.application.spi.RoomRepository;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.BookingStatus;
import com.dess.scheduling.domain.ResourceRef;
import com.dess.scheduling.domain.Room;

class BookingServiceTest
{
    private InMemoryBookingRepository bookings;
    private InMemoryBookingSlotRepository slots;
    private InMemoryRoomRepository rooms;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp()
    {
        bookings = new InMemoryBookingRepository();
        slots = new InMemoryBookingSlotRepository();
        rooms = new InMemoryRoomRepository();
        service = new BookingServiceImpl(bookings, slots, rooms);
    }

    @Test
    void createsBookingWithOneResource()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID ownerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        Booking created = service.create("SESSION", ownerId, start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId, 10)));

        assertEquals("SESSION", created.ownerType());
        assertEquals(ownerId, created.ownerId());
        assertEquals(1, service.findSlotsByBookingId(created.id()).size());
        assertEquals(created, service.findById(created.id()).orElseThrow());
    }

    @Test
    void createsBookingWithMultipleResources()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        Booking created = service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(
                        new BookingService.ResourceRequest("ROOM", roomId, 10),
                        new BookingService.ResourceRequest("EQUIPMENT", UUID.randomUUID(), 0)
                ));

        assertEquals(2, service.findSlotsByBookingId(created.id()).size());
    }

    @Test
    void rejectsBookingWithoutResource()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start, start.plusHours(1), List.of()));

        assertEquals("at least one resource is required", exception.getMessage());
    }

    @Test
    void rejectsOverlappingResource()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId, 10)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start.plusMinutes(30),
                        end.plusMinutes(30),
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));

        assertEquals("resource is already booked for this time range", exception.getMessage());
    }

    @Test
    void considersAdjacentSlotAvailable()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId, 10)));

        assertTrue(service.isAvailable("ROOM", roomId, end, end.plusHours(1)));
        assertFalse(service.isAvailable("ROOM", roomId, start.plusMinutes(30), end.plusMinutes(30)));
    }

    @Test
    void cancelsBookingAndItsSlots()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        Booking created = service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId, 10)));

        Booking cancelled = service.cancel(created.id());

        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertTrue(service.findSlotsByBookingId(created.id()).stream()
                .allMatch(slot -> slot.status() == BookingStatus.CANCELLED));
    }

    @Test
    void rejectsUnknownBookingCancellation()
    {
        UUID unknownId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.cancel(unknownId));

        assertEquals("booking not found: " + unknownId, exception.getMessage());
    }

    @Test
    void findsBookingsByOwner()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        UUID ownerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        Booking created = service.create("SESSION", ownerId, start, start.plusHours(1),
                List.of(new BookingService.ResourceRequest("ROOM", roomId, 10)));

        assertEquals(List.of(created), service.findByOwner("SESSION", ownerId));
    }

    @Test
    void createsBookingFromTypedRequest()
    {
        OffsetDateTime start = date("2026-11-23T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-23T11:00:00+03:00");
        UUID ownerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        Booking created = service.create(new BookingRequest(BookingService.EXAM_SESSION_OWNER,
                ownerId, start, end,
                List.of(ResourceRef.room(roomId),
                        ResourceRef.personId(42L),
                        ResourceRef.studentGroup(UUID.randomUUID()))));

        assertEquals(BookingService.EXAM_SESSION_OWNER, created.ownerType());
        assertEquals(ownerId, created.ownerId());
        assertEquals(3, service.findSlotsByBookingId(created.id()).size());
        assertEquals(created, service.findById(created.id()).orElseThrow());
    }

    @Test
    void checksAvailabilityWithTypedRef()
    {
        OffsetDateTime start = date("2026-11-23T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-23T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();

        service.create(new BookingRequest(BookingService.TEACHING_SESSION_OWNER,
                UUID.randomUUID(), start, end, List.of(ResourceRef.room(roomId))));

        assertFalse(service.isAvailable(ResourceRef.room(roomId),
                start.plusMinutes(30), end.plusMinutes(30)));
        assertTrue(service.isAvailable(ResourceRef.room(roomId), end, end.plusHours(1)));
        assertTrue(service.isAvailable(ResourceRef.room(UUID.randomUUID()), start, end));
    }

    @Test
    void typedPersonRefMapsToSameResource()
    {
        OffsetDateTime start = date("2026-11-23T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-23T11:00:00+03:00");

        service.create(new BookingRequest(BookingService.EXAM_SESSION_OWNER,
                UUID.randomUUID(), start, end, List.of(ResourceRef.personId(42L))));

        assertFalse(service.isAvailable(ResourceRef.personId(42L), start, end));
        assertTrue(service.isAvailable(ResourceRef.personId(43L), start, end));
    }

    @Test
    void acceptsBookingWhenRequiredCapacityEqualsRoomCapacity()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        Booking created = service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId, 30)));

        assertEquals(1, service.findSlotsByBookingId(created.id()).size());
    }

    @Test
    void rejectsBookingWhenRequiredCapacityExceedsRoomCapacity()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start, end,
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 40))));

        assertEquals("room capacity is insufficient: room A101 holds 30, 40 requested",
                exception.getMessage());
    }

    @Test
    void rejectsRoomBookingWithoutRequiredCapacity()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        UUID roomId = UUID.randomUUID();
        rooms.save(new Room(roomId, "A101", "Salle A101", 30));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start, start.plusHours(1),
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 0))));

        assertEquals("a room booking must state the required capacity", exception.getMessage());
    }

    @Test
    void rejectsBookingForUnknownRoomWhenCapacityIsRequired()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        UUID roomId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start, start.plusHours(1),
                        List.of(new BookingService.ResourceRequest("ROOM", roomId, 10))));

        assertEquals("room not found: " + roomId, exception.getMessage());
    }

    @Test
    void rejectsNegativeRequiredCapacity()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start, start.plusHours(1),
                        List.of(new BookingService.ResourceRequest("ROOM", UUID.randomUUID(), -1))));

        assertEquals("required capacity cannot be negative", exception.getMessage());
    }

    private static OffsetDateTime date(String value)
    {
        return OffsetDateTime.parse(value);
    }

    private static final class InMemoryBookingRepository implements BookingRepository
    {
        private final List<Booking> values = new ArrayList<>();

        @Override
        public Booking save(Booking booking)
        {
            values.removeIf(existing -> existing.id().equals(booking.id()));
            values.add(booking);
            return booking;
        }

        @Override
        public Optional<Booking> findById(UUID id)
        {
            return values.stream().filter(booking -> booking.id().equals(id)).findFirst();
        }

        @Override
        public List<Booking> findAll()
        {
            return List.copyOf(values);
        }

        @Override
        public List<Booking> findByOwner(String ownerType, UUID ownerId)
        {
            return values.stream()
                    .filter(booking -> booking.ownerType().equals(ownerType))
                    .filter(booking -> booking.ownerId().equals(ownerId))
                    .toList();
        }
    }

    private static final class InMemoryBookingSlotRepository implements BookingSlotRepository
    {
        private final List<BookingSlot> values = new ArrayList<>();

        @Override
        public BookingSlot save(BookingSlot slot)
        {
            values.removeIf(existing -> existing.id().equals(slot.id()));
            values.add(slot);
            return slot;
        }

        @Override
        public Optional<BookingSlot> findById(UUID id)
        {
            return values.stream().filter(slot -> slot.id().equals(id)).findFirst();
        }

        @Override
        public List<BookingSlot> findByBookingId(UUID bookingId)
        {
            return values.stream()
                    .filter(slot -> slot.bookingId().equals(bookingId))
                    .toList();
        }

        @Override
        public boolean hasOverlap(String resourceType, UUID resourceId,
                                  OffsetDateTime startsAt, OffsetDateTime endsAt)
        {
            return values.stream()
                    .filter(slot -> slot.status() == BookingStatus.ACTIVE)
                    .filter(slot -> slot.resourceType().equals(resourceType))
                    .filter(slot -> slot.resourceId().equals(resourceId))
                    .anyMatch(slot -> slot.startsAt().isBefore(endsAt)
                            && slot.endsAt().isAfter(startsAt));
        }
    }

    private static final class InMemoryRoomRepository implements RoomRepository
    {
        private final List<Room> values = new ArrayList<>();

        @Override
        public Room save(Room room)
        {
            values.removeIf(existing -> existing.id().equals(room.id()));
            values.add(room);
            return room;
        }

        @Override
        public Optional<Room> findById(UUID id)
        {
            return values.stream().filter(room -> room.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Room> findByCode(String code)
        {
            return values.stream().filter(room -> room.code().equals(code)).findFirst();
        }

        @Override
        public List<Room> findAll()
        {
            return List.copyOf(values);
        }
    }
}