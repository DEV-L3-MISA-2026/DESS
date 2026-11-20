package com.dess.scheduling.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;

public interface BookingService
{
    Booking create(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                   OffsetDateTime endsAt, List<ResourceRequest> resources);

    Booking cancel(UUID bookingId);

    Optional<Booking> findById(UUID id);

    List<Booking> findAll();

    List<Booking> findByOwner(String ownerType, UUID ownerId);

    List<BookingSlot> findSlotsByBookingId(UUID bookingId);

    boolean isAvailable(String resourceType, UUID resourceId,
                        OffsetDateTime startsAt, OffsetDateTime endsAt);

    record ResourceRequest(String resourceType, UUID resourceId) {}
}
