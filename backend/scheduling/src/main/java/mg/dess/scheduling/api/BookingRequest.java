package mg.dess.scheduling.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Request to reserve resources for the half-open range [startsAt, endsAt).
 *
 * @param requiredCapacity number of people the requested room must hold; null skips the
 *                         capacity check (proposed addition, see docs/contracts/booking-contract.md)
 */
public record BookingRequest(
        UUID ownerId,
        BookingOwnerType ownerType,
        List<ResourceRef> resources,
        Instant startsAt,
        Instant endsAt,
        Integer requiredCapacity) {

    public BookingRequest {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(ownerType, "ownerType");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        if (resources == null || resources.isEmpty()) {
            throw new IllegalArgumentException("at least one resource is required");
        }
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
        if (requiredCapacity != null && requiredCapacity < 0) {
            throw new IllegalArgumentException("requiredCapacity must not be negative");
        }
        resources = List.copyOf(resources);
    }
}
