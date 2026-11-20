package com.dess.scheduling.application;

import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.Room;

public interface SchedulingService
{
    RoomService rooms();

    BookingService bookings();

    Optional<Room> findRoom(UUID id);

    Optional<Booking> findBooking(UUID id);
}
