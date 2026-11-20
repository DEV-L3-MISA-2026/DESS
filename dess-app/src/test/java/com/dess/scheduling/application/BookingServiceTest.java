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
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.BookingStatus;

class BookingServiceTest
{
    private InMemoryBookingRepository bookings;
    private InMemoryBookingSlotRepository slots;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp()
    {
        bookings = new InMemoryBookingRepository();
        slots = new InMemoryBookingSlotRepository();
        service = new BookingServiceImpl(bookings, slots);
    }

    @Test
    void createsBookingWithOneResource()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID ownerId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        Booking created = service.create("SESSION", ownerId, start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId)));

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

        Booking created = service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(
                        new BookingService.ResourceRequest("ROOM", UUID.randomUUID()),
                        new BookingService.ResourceRequest("EQUIPMENT", UUID.randomUUID())
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

        service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("SESSION", UUID.randomUUID(), start.plusMinutes(30),
                        end.plusMinutes(30),
                        List.of(new BookingService.ResourceRequest("ROOM", roomId))));

        assertEquals("resource is already booked for this time range", exception.getMessage());
    }

    @Test
    void considersAdjacentSlotAvailable()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");
        UUID roomId = UUID.randomUUID();

        service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", roomId)));

        assertTrue(service.isAvailable("ROOM", roomId, end, end.plusHours(1)));
        assertFalse(service.isAvailable("ROOM", roomId, start.plusMinutes(30), end.plusMinutes(30)));
    }

    @Test
    void cancelsBookingAndItsSlots()
    {
        OffsetDateTime start = date("2026-11-20T09:00:00+03:00");
        OffsetDateTime end = date("2026-11-20T11:00:00+03:00");

        Booking created = service.create("SESSION", UUID.randomUUID(), start, end,
                List.of(new BookingService.ResourceRequest("ROOM", UUID.randomUUID())));

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

        Booking created = service.create("SESSION", ownerId, start, start.plusHours(1),
                List.of(new BookingService.ResourceRequest("ROOM", UUID.randomUUID())));

        assertEquals(List.of(created), service.findByOwner("SESSION", ownerId));
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
}
