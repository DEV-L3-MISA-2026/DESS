package com.dess.scheduling.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.scheduling.domain.Room;

public interface RoomService
{
    Room create(String code, String name, int capacity);

    Room update(UUID id, String name, int capacity);

    Optional<Room> findById(UUID id);

    Optional<Room> findByCode(String code);

    List<Room> findAll();
}
