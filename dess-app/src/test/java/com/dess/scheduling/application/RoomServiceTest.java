package com.dess.scheduling.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dess.scheduling.application.impl.RoomServiceImpl;
import com.dess.scheduling.application.spi.RoomRepository;
import com.dess.scheduling.domain.Room;

class RoomServiceTest
{
    private InMemoryRoomRepository repository;
    private RoomServiceImpl service;

    @BeforeEach
    void setUp()
    {
        repository = new InMemoryRoomRepository();
        service = new RoomServiceImpl(repository);
    }

    @Test
    void createsAndFindsRoom()
    {
        Room created = service.create("A101", "Amphitheatre", 80);

        assertEquals("A101", created.code());
        assertEquals(created, service.findById(created.id()).orElseThrow());
        assertEquals(created, service.findByCode("A101").orElseThrow());
        assertEquals(List.of(created), service.findAll());
    }

    @Test
    void rejectsDuplicateRoomCode()
    {
        service.create("A101", "Amphitheatre", 80);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create("A101", "Lab 1", 40));

        assertEquals("room code already exists: A101", exception.getMessage());
    }

    @Test
    void updatesExistingRoom()
    {
        Room created = service.create("A101", "Amphitheatre", 80);

        Room updated = service.update(created.id(), "New amphitheatre", 100);

        assertEquals(created.id(), updated.id());
        assertEquals("A101", updated.code());
        assertEquals("New amphitheatre", updated.name());
        assertEquals(100, updated.capacity());
    }

    @Test
    void rejectsUpdateOfUnknownRoom()
    {
        UUID unknownId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.update(unknownId, "Unknown", 20));

        assertEquals("room not found: " + unknownId, exception.getMessage());
    }

    private static final class InMemoryRoomRepository implements RoomRepository
    {
        private final List<Room> rooms = new ArrayList<>();

        @Override
        public Room save(Room room)
        {
            rooms.removeIf(existing -> existing.id().equals(room.id()));
            rooms.add(room);
            return room;
        }

        @Override
        public Optional<Room> findById(UUID id)
        {
            return rooms.stream().filter(room -> room.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Room> findByCode(String code)
        {
            return rooms.stream().filter(room -> room.code().equals(code)).findFirst();
        }

        @Override
        public List<Room> findAll()
        {
            return List.copyOf(rooms);
        }
    }
}
