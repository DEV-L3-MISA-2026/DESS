package com.dess.scheduling.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.dess.scheduling.domain.ResourceRef;

/**
 * A frozen booking request: everything a booking needs, in one value.
 *
 * <p>Rules of the frozen contract:</p>
 * <ul>
 *   <li>ownerType names the owner, for example "SESSION" for a teaching
 *       session or "EXAM_SESSION" for an exam session. It is at most
 *       30 characters, like the booking table column.</li>
 *   <li>The time range is half open, [startsAt, endsAt). A booking that
 *       starts exactly when another one ends does not overlap it.</li>
 *   <li>Instants are stored in UTC. Any offset given by a caller is
 *       converted to UTC on construction, so two requests for the same
 *       instant compare equal whatever offset the caller used.</li>
 *   <li>At least one resource is required. Resources are
 *       {@link ResourceRef} values, so their type is one of the frozen
 *       resource types and their id is never null.</li>
 * </ul>
 */
public record BookingRequest(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                             OffsetDateTime endsAt, List<ResourceRef> resources)
{
    public BookingRequest
    {
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
        Objects.requireNonNull(resources, "resources are required");
        if (resources.isEmpty())
        {
            throw new IllegalArgumentException("at least one resource is required");
        }
        if (resources.stream().anyMatch(Objects::isNull))
        {
            throw new IllegalArgumentException("resource is required");
        }
        startsAt = startsAt.withOffsetSameInstant(ZoneOffset.UTC);
        endsAt = endsAt.withOffsetSameInstant(ZoneOffset.UTC);
        resources = List.copyOf(resources);
    }
}
