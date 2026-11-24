package com.dess.scheduling.application.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.dess.scheduling.application.BookingRequest;
import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.spi.BookingRepository;
import com.dess.scheduling.application.spi.BookingSlotRepository;
import com.dess.scheduling.application.spi.RoomRepository;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.Room;

@ApplicationScoped
@Transactional
public class BookingServiceImpl implements BookingService
{
    private final BookingRepository bookings;
    private final BookingSlotRepository slots;
    private final RoomRepository rooms;

    @Inject
    public BookingServiceImpl(BookingRepository bookings, BookingSlotRepository slots,
                              RoomRepository rooms)
    {
        this.bookings = bookings;
        this.slots = slots;
        this.rooms = rooms;
    }

    @Override
    public Booking create(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                          OffsetDateTime endsAt, List<ResourceRequest> resources)
    {
        return createInternal(ownerType, ownerId, startsAt, endsAt, resources, true);
    }

    /**
     * The frozen {@link BookingRequest} carries {@code ResourceRef} values,
     * which state no required capacity, so no capacity check can apply on
     * this path: the check only runs for callers that state a capacity
     * through {@link ResourceRequest}.
     */
    @Override
    public Booking create(BookingRequest request)
    {
        List<ResourceRequest> resources = request.resources().stream()
                .map(ref -> new ResourceRequest(ref.typeName(), ref.id()))
                .toList();
        return createInternal(request.ownerType(), request.ownerId(), request.startsAt(),
                request.endsAt(), resources, false);
    }

    private Booking createInternal(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                                   OffsetDateTime endsAt, List<ResourceRequest> resources,
                                   boolean enforceCapacity)
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
            if (enforceCapacity)
            {
                checkCapacity(resource);
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

    private void checkCapacity(ResourceRequest resource)
    {
        if (resource.requiredCapacity() < 0)
        {
            throw new IllegalArgumentException("required capacity cannot be negative");
        }
        if (!"ROOM".equals(resource.resourceType()))
        {
            return;
        }
        if (resource.requiredCapacity() == 0)
        {
            throw new IllegalArgumentException("a room booking must state the required capacity");
        }
        Room room = rooms.findById(resource.resourceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "room not found: " + resource.resourceId()));
        if (resource.requiredCapacity() > room.capacity())
        {
            throw new IllegalArgumentException("room capacity is insufficient: room " + room.code()
                    + " holds " + room.capacity() + ", " + resource.requiredCapacity() + " requested");
        }
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
    public List<Booking> findAll()
    {
        return bookings.findAll();
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