package com.dess.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.Room;

class SchedulingDomainTest
{
    @Test
    void createsValidRoomAndBookingSlot()
    {
        Room room = Room.create("A101", "Amphitheatre", 80);
        OffsetDateTime start = OffsetDateTime.parse("2026-11-18T09:00:00+03:00");
        OffsetDateTime end = start.plusHours(2);

        Booking booking = Booking.create("SESSION", UUID.randomUUID(), start, end);
        BookingSlot slot = BookingSlot.create(booking.id(), "ROOM", room.id(), start, end);

        assertEquals("A101", room.code());
        assertEquals(booking.id(), slot.bookingId());
    }

    @Test
    void rejectsInvalidRoomCapacity()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Room.create("A101", "Amphitheatre", 0));
        assertEquals("capacity must be greater than zero", exception.getMessage());
    }

    @Test
    void rejectsInvalidTimeRange()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-18T09:00:00+03:00");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Booking.create("SESSION", UUID.randomUUID(), start, start));
        assertEquals("startsAt must be before endsAt", exception.getMessage());
    }

    @Test
    void cancellationChangesBookingAndSlotStatus()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-18T09:00:00+03:00");
        OffsetDateTime end = start.plusHours(1);
        Booking booking = Booking.create("SESSION", UUID.randomUUID(), start, end);
        BookingSlot slot = BookingSlot.create(booking.id(), "ROOM", UUID.randomUUID(), start, end);

        assertEquals("CANCELLED", booking.cancel().status().name());
        assertEquals("CANCELLED", slot.cancel().status().name());
    }
}
