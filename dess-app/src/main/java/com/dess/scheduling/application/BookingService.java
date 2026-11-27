package com.dess.scheduling.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.ResourceRef;
import java.time.LocalDate;

/**
 * Booking of shared resources for teaching sessions and exam sessions.
 *
 * <p>
 * Frozen contract, agreed for the DESS MVP. A booking reserves one
 * or more resources for a half open time range, [startsAt, endsAt).
 * A booking that starts exactly when another one ends does not
 * overlap it. All instants are stored in UTC. The resource types are
 * the frozen set in the scheduling domain: ROOM, PERSON and
 * STUDENT_GROUP. Creating a booking is atomic: either every resource
 * is reserved, or nothing is reserved and the call fails. Cancelling
 * a booking frees all of its slots.
 * </p>
 *
 * <p>
 * Callers should use {@link BookingRequest} and {@link ResourceRef}.
 * The String based methods stay for the existing callers and map to
 * the same behaviour.
 * </p>
 */
public interface BookingService {
    /** Owner type of a booking made for a teaching session. */
    String TEACHING_SESSION_OWNER = "SESSION";

    /** Owner type of a booking made for an exam session. */
    String EXAM_SESSION_OWNER = "EXAM_SESSION";

    Booking create(String ownerType, UUID ownerId, OffsetDateTime startsAt,
            OffsetDateTime endsAt, List<ResourceRequest> resources);

    /**
     * Creates a booking from a frozen {@link BookingRequest}.
     * The request already carries the validated owner, the UTC time
     * range and the typed resources.
     */
    default Booking create(BookingRequest request) {
        List<ResourceRequest> resources = request.resources().stream()
                .map(ref -> new ResourceRequest(ref.typeName(), ref.id()))
                .toList();
        return create(request.ownerType(), request.ownerId(), request.startsAt(),
                request.endsAt(), resources);
    }

    Booking cancel(UUID bookingId);

    Optional<Booking> findById(UUID id);

    List<Booking> findAll();

    List<Booking> findByOwner(String ownerType, UUID ownerId);

    List<BookingSlot> findSlotsByBookingId(UUID bookingId);

    boolean isAvailable(String resourceType, UUID resourceId,
            OffsetDateTime startsAt, OffsetDateTime endsAt);

    /**
     * Returns true when the given resource is free for the whole half
     * open range [startsAt, endsAt). A range that only touches an
     * existing booking at its end is free.
     */
    default boolean isAvailable(ResourceRef resource, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        return isAvailable(resource.typeName(), resource.id(), startsAt, endsAt);
    }

    /**
     * One resource to reserve. {@code requiredCapacity} is the number of
     * people the resource must hold; it is only checked for a ROOM, where
     * a caller using this String based form must state it. The two
     * argument constructor states no capacity (0) and stays for the
     * existing callers and for the frozen {@link BookingRequest} mapping,
     * whose {@link ResourceRef} carries no capacity.
     */
    record ResourceRequest(String resourceType, UUID resourceId, int requiredCapacity) {
        public ResourceRequest(String resourceType, UUID resourceId) {
            this(resourceType, resourceId, 0);
        }
    }
     /**
     * Taken and free intervals of one room for one day (UTC). A booking
     * ending at 10:00 frees the room from 10:00: intervals are half-open.
     */
    RoomAvailability findRoomAvailability(UUID roomId, LocalDate day);

}
