package mg.dess.scheduling.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proof that all requested resources were reserved. */
public record BookingReceipt(
        UUID bookingId,
        List<ResourceRef> resources,
        Instant startsAt,
        Instant endsAt) {
}
