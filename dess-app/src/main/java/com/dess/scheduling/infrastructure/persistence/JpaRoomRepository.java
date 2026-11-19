package com.dess.scheduling.infrastructure.persistence;

import com.dess.scheduling.application.spi.RoomRepository;
import com.dess.scheduling.domain.Room;
import com.dess.scheduling.infrastructure.persistence.entity.RoomEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaRoomRepository implements RoomRepository
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Override
    public Room save(Room room)
    {
        RoomEntity entity = entityManager.find(RoomEntity.class, room.id());
        if (entity == null)
        {
            entity = new RoomEntity(room.id(), room.code(), room.name(), room.capacity());
            entityManager.persist(entity);
        }
        else
        {
            entity.update(room.name(), room.capacity());
        }
        return toDomain(entity);
    }

    @Override
    public Optional<Room> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(RoomEntity.class, id)).map(this::toDomain);
    }

    @Override
    public Optional<Room> findByCode(String code)
    {
        List<RoomEntity> result = entityManager.createQuery(
                        "select r from RoomEntity r where r.code = :code", RoomEntity.class)
                .setParameter("code", code)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<Room> findAll()
    {
        return entityManager.createQuery("select r from RoomEntity r", RoomEntity.class)
                .getResultList().stream().map(this::toDomain).toList();
    }

    private Room toDomain(RoomEntity entity)
    {
        return new Room(entity.getId(), entity.getCode(), entity.getName(), entity.getCapacity());
    }
}
