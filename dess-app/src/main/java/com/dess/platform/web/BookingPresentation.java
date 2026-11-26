package com.dess.platform.web;

import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.BookingStatus;
import com.dess.scheduling.domain.ResourceType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only projections of bookings for the server-rendered pages.
 *
 * <p>They keep the selection and the labels out of the servlets so the
 * behaviour can be tested without a servlet container.</p>
 */
public final class BookingPresentation
{
    private BookingPresentation()
    {
    }

    /**
     * Selects the active bookings that start at or after {@code now},
     * ordered by start time and capped at {@code limit}.
     *
     * @param bookings bookings to filter
     * @param now the current instant, inclusive
     * @param limit maximum number of rows
     * @return the upcoming bookings, earliest first
     * @throws IllegalArgumentException when {@code limit} is not positive
     */
    public static List<Booking> upcoming(List<Booking> bookings, OffsetDateTime now, int limit)
    {
        Objects.requireNonNull(bookings, "bookings is required");
        Objects.requireNonNull(now, "now is required");

        if (limit <= 0)
        {
            throw new IllegalArgumentException("limit must be greater than zero");
        }

        return bookings.stream()
                .filter(booking -> booking.status() == BookingStatus.ACTIVE)
                .filter(booking -> !booking.startsAt().isBefore(now))
                .sorted(Comparator.comparing(Booking::startsAt).thenComparing(Booking::id))
                .limit(limit)
                .toList();
    }

    /**
     * Describes the resources reserved by a booking, using room codes so
     * the reader does not have to decode identifiers.
     *
     * @param slots booking slots, usually from {@code findSlotsByBookingId}
     * @param roomCodes room id to room code, used to label room slots
     * @return a short text such as {@code Room A101, 2 people}
     */
    public static String resources(List<BookingSlot> slots, Map<UUID, String> roomCodes)
    {
        Objects.requireNonNull(slots, "slots is required");
        Objects.requireNonNull(roomCodes, "roomCodes is required");

        List<String> labels = new ArrayList<>();
        int people = 0;
        int groups = 0;

        for (BookingSlot slot : slots)
        {
            if (ResourceType.ROOM.name().equals(slot.resourceType()))
            {
                labels.add("Room " + roomCodes.getOrDefault(slot.resourceId(), "unknown"));
            }
            else if (ResourceType.PERSON.name().equals(slot.resourceType()))
            {
                people++;
            }
            else if (ResourceType.STUDENT_GROUP.name().equals(slot.resourceType()))
            {
                groups++;
            }
        }

        if (people > 0)
        {
            labels.add(people + (people == 1 ? " person" : " people"));
        }
        if (groups > 0)
        {
            labels.add(groups + (groups == 1 ? " student group" : " student groups"));
        }

        return labels.isEmpty() ? "No resource recorded" : String.join(", ", labels);
    }

    /**
     * Names the owner of a booking without showing its identifier.
     *
     * @param ownerType owner type stored on the booking
     * @return a readable owner label
     */
    public static String ownerLabel(String ownerType)
    {
        return switch (ownerType)
        {
            case "SESSION" -> "Teaching session";
            case "EXAM_SESSION" -> "Exam session";
            default -> ownerType;
        };
    }
}
