package com.dess.scheduling.application.impl;

import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.RoomService;
import com.dess.scheduling.application.SchedulingService;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.Room;

public class SchedulingServiceImpl implements SchedulingService
{
    private final RoomService rooms;
    private final BookingService bookings;

    public SchedulingServiceImpl(RoomService rooms, BookingService bookings)
    {
        this.rooms = rooms;
        this.bookings = bookings;
    }

    @Override
    public RoomService rooms()
    {
        return rooms;
    }

    @Override
    public BookingService bookings()
    {
        return bookings;
    }

    @Override
    public Optional<Room> findRoom(UUID id)
    {
        return rooms.findById(id);
    }

    @Override
    public Optional<Booking> findBooking(UUID id)
    {
        return bookings.findById(id);
    }
}
