package com.dess.scheduling.application.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.Booking;

public interface BookingRepository
{
    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    List<Booking> findAll();

    List<Booking> findByOwner(String ownerType, UUID ownerId);
}
