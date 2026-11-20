package com.dess.scheduling.application.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.dess.scheduling.application.RoomService;
import com.dess.scheduling.application.spi.RoomRepository;
import com.dess.scheduling.domain.Room;

@ApplicationScoped
@Transactional
public class RoomServiceImpl implements RoomService
{
    private final RoomRepository rooms;

    @Inject
    public RoomServiceImpl(RoomRepository rooms)
    {
        this.rooms = rooms;
    }

    @Override
    public Room create(String code, String name, int capacity)
    {
        Room room = Room.create(code, name, capacity);
        if (rooms.findByCode(code).isPresent())
        {
            throw new IllegalArgumentException("room code already exists: " + code);
        }
        return rooms.save(room);
    }

    @Override
    public Room update(UUID id, String name, int capacity)
    {
        Room room = rooms.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("room not found: " + id));
        return rooms.save(new Room(room.id(), room.code(), name, capacity));
    }

    @Override
    public Optional<Room> findById(UUID id)
    {
        return rooms.findById(id);
    }

    @Override
    public Optional<Room> findByCode(String code)
    {
        return rooms.findByCode(code);
    }

    @Override
    public List<Room> findAll()
    {
        return rooms.findAll();
    }
}
