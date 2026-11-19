package com.dess.scheduling.application.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.transaction.Transactional;

import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.spi.BookingRepository;
import com.dess.scheduling.application.spi.BookingSlotRepository;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;

@Transactional
public class BookingServiceImpl implements BookingService
{
    private final BookingRepository bookings;
    private final BookingSlotRepository slots;

    public BookingServiceImpl(BookingRepository bookings, BookingSlotRepository slots)
    {
        this.bookings = bookings;
        this.slots = slots;
    }

    @Override
    public Booking create(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                          OffsetDateTime endsAt, List<ResourceRequest> resources)
    {
        Booking booking = Booking.create(ownerType, ownerId, startsAt, endsAt);
        if (resources == null || resources.isEmpty())
        {
            throw new IllegalArgumentException("at least one resource is required");
        }

        for (ResourceRequest resource : resources)
        {
            if (resource == null)
            {
                throw new IllegalArgumentException("resource is required");
            }
            if (slots.hasOverlap(resource.resourceType(), resource.resourceId(), startsAt, endsAt))
            {
                throw new IllegalArgumentException("resource is already booked for this time range");
            }
        }

        Booking saved = bookings.save(booking);
        for (ResourceRequest resource : resources)
        {
            slots.save(BookingSlot.create(saved.id(), resource.resourceType(),
                    resource.resourceId(), startsAt, endsAt));
        }
        return saved;
    }

    @Override
    public Booking cancel(UUID bookingId)
    {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("booking not found: " + bookingId));

        Booking cancelled = booking.cancel();
        for (BookingSlot slot : slots.findByBookingId(bookingId))
        {
            slots.save(slot.cancel());
        }
        return bookings.save(cancelled);
    }

    @Override
    public Optional<Booking> findById(UUID id)
    {
        return bookings.findById(id);
    }

    @Override
    public List<Booking> findByOwner(String ownerType, UUID ownerId)
    {
        return bookings.findByOwner(ownerType, ownerId);
    }

    @Override
    public List<BookingSlot> findSlotsByBookingId(UUID bookingId)
    {
        return slots.findByBookingId(bookingId);
    }

    @Override
    public boolean isAvailable(String resourceType, UUID resourceId,
                               OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        return !slots.hasOverlap(resourceType, resourceId, startsAt, endsAt);
    }
}
