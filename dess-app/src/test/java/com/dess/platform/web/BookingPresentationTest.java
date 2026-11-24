package com.dess.platform.web;

import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingPresentationTest
{
    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void upcomingKeepsActiveFutureBookingsEarliestFirst()
    {
        Booking late = booking(NOW.plusHours(4));
        Booking soon = booking(NOW.plusHours(1));

        List<Booking> result = BookingPresentation.upcoming(List.of(late, soon), NOW, 6);

        assertEquals(List.of(soon, late), result);
    }

    @Test
    void upcomingIncludesABookingThatStartsNow()
    {
        Booking current = booking(NOW);

        List<Booking> result = BookingPresentation.upcoming(List.of(current), NOW, 6);

        assertEquals(List.of(current), result);
    }

    @Test
    void upcomingDropsPastAndCancelledBookings()
    {
        Booking past = booking(NOW.minusHours(1));
        Booking cancelled = booking(NOW.plusHours(2)).cancel();
        Booking upcoming = booking(NOW.plusHours(3));

        List<Booking> result = BookingPresentation.upcoming(List.of(past, cancelled, upcoming), NOW, 6);

        assertEquals(List.of(upcoming), result);
    }

    @Test
    void upcomingCapsTheList()
    {
        List<Booking> bookings = List.of(
                booking(NOW.plusHours(1)),
                booking(NOW.plusHours(2)),
                booking(NOW.plusHours(3)),
                booking(NOW.plusHours(4)),
                booking(NOW.plusHours(5)),
                booking(NOW.plusHours(6)),
                booking(NOW.plusHours(7)));

        List<Booking> result = BookingPresentation.upcoming(bookings, NOW, 6);

        assertEquals(6, result.size());
        assertEquals(NOW.plusHours(1), result.get(0).startsAt());
        assertEquals(NOW.plusHours(6), result.get(5).startsAt());
    }

    @Test
    void upcomingRejectsANonPositiveLimit()
    {
        assertThrows(IllegalArgumentException.class,
                () -> BookingPresentation.upcoming(List.of(), NOW, 0));
    }

    @Test
    void resourcesNamesRoomsAndCountsOtherResources()
    {
        UUID bookingId = UUID.randomUUID();
        UUID knownRoom = UUID.randomUUID();
        UUID unknownRoom = UUID.randomUUID();
        OffsetDateTime end = NOW.plusHours(2);

        List<BookingSlot> slots = List.of(
                BookingSlot.create(bookingId, "ROOM", knownRoom, NOW, end),
                BookingSlot.create(bookingId, "ROOM", unknownRoom, NOW, end),
                BookingSlot.create(bookingId, "PERSON", UUID.randomUUID(), NOW, end),
                BookingSlot.create(bookingId, "PERSON", UUID.randomUUID(), NOW, end),
                BookingSlot.create(bookingId, "STUDENT_GROUP", UUID.randomUUID(), NOW, end));

        String result = BookingPresentation.resources(slots, Map.of(knownRoom, "A101"));

        assertEquals("Room A101, Room unknown, 2 people, 1 student group", result);
    }

    @Test
    void resourcesUsesSingularForOnePerson()
    {
        UUID bookingId = UUID.randomUUID();

        List<BookingSlot> slots = List.of(
                BookingSlot.create(bookingId, "PERSON", UUID.randomUUID(), NOW, NOW.plusHours(1)));

        assertEquals("1 person", BookingPresentation.resources(slots, Map.of()));
    }

    @Test
    void resourcesExplainsWhenNoResourceIsRecorded()
    {
        assertEquals("No resource recorded", BookingPresentation.resources(List.of(), Map.of()));
    }

    @Test
    void ownerLabelHidesInternalOwnerTypes()
    {
        assertEquals("Teaching session", BookingPresentation.ownerLabel("SESSION"));
        assertEquals("Exam session", BookingPresentation.ownerLabel("EXAM_SESSION"));
        assertEquals("OTHER", BookingPresentation.ownerLabel("OTHER"));
    }

    private Booking booking(OffsetDateTime startsAt)
    {
        return Booking.create("SESSION", UUID.randomUUID(), startsAt, startsAt.plusHours(1));
    }
}
