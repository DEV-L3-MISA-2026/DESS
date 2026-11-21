package com.dess.scheduling.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** @brief A booking owned by an academic or scheduling resource. */
public record Booking(
        UUID id,
        String ownerType,
        UUID ownerId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BookingStatus status,
        OffsetDateTime createdAt)
{
    public Booking
    {
        Objects.requireNonNull(id, "id is required");
        if (ownerType == null || ownerType.isBlank())
        {
            throw new IllegalArgumentException("ownerType is required");
        }
        if (ownerType.length() > 30)
        {
            throw new IllegalArgumentException("ownerType must contain at most 30 characters");
        }
        Objects.requireNonNull(ownerId, "ownerId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
        Objects.requireNonNull(endsAt, "endsAt is required");
        if (!startsAt.isBefore(endsAt))
        {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
        Objects.requireNonNull(status, "status is required");
    }

    public static Booking create(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                                 OffsetDateTime endsAt)
    {
        return new Booking(UUID.randomUUID(), ownerType, ownerId, startsAt, endsAt,
                BookingStatus.ACTIVE, OffsetDateTime.now());
    }

    public Booking cancel()
    {
        return new Booking(id, ownerType, ownerId, startsAt, endsAt,
                BookingStatus.CANCELLED, createdAt);
    }
}
