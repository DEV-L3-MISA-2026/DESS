package com.dess.scheduling.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** @brief A time range reserved for one concrete resource. */
public record BookingSlot(
        UUID id,
        UUID bookingId,
        String resourceType,
        UUID resourceId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BookingStatus status)
{
    public BookingSlot
    {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(bookingId, "bookingId is required");
        if (resourceType == null || resourceType.isBlank())
        {
            throw new IllegalArgumentException("resourceType is required");
        }
        if (resourceType.length() > 30)
        {
            throw new IllegalArgumentException("resourceType must contain at most 30 characters");
        }
        Objects.requireNonNull(resourceId, "resourceId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
        Objects.requireNonNull(endsAt, "endsAt is required");
        if (!startsAt.isBefore(endsAt))
        {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
        Objects.requireNonNull(status, "status is required");
    }

    public static BookingSlot create(UUID bookingId, String resourceType, UUID resourceId,
                                     OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        return new BookingSlot(UUID.randomUUID(), bookingId, resourceType, resourceId,
                startsAt, endsAt, BookingStatus.ACTIVE);
    }

    public BookingSlot cancel()
    {
        return new BookingSlot(id, bookingId, resourceType, resourceId,
                startsAt, endsAt, BookingStatus.CANCELLED);
    }
}
