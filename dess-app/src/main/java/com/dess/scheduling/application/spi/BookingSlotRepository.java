package com.dess.scheduling.application.spi;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.BookingSlot;

public interface BookingSlotRepository
{
    BookingSlot save(BookingSlot slot);

    Optional<BookingSlot> findById(UUID id);

    List<BookingSlot> findByBookingId(UUID bookingId);

    boolean hasOverlap(String resourceType, UUID resourceId,
                       OffsetDateTime startsAt, OffsetDateTime endsAt);
}
