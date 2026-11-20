package com.dess.scheduling.application.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.Room;

public interface RoomRepository
{
    Room save(Room room);

    Optional<Room> findById(UUID id);

    Optional<Room> findByCode(String code);

    List<Room> findAll();
}
