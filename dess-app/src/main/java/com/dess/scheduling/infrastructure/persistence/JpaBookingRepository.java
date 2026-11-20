package com.dess.scheduling.infrastructure.persistence;

import com.dess.scheduling.application.spi.BookingRepository;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingStatus;
import com.dess.scheduling.infrastructure.persistence.entity.BookingEntity;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaBookingRepository implements BookingRepository
{
    private final EntityManager entityManager;

    public JpaBookingRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public Booking save(Booking booking)
    {
        BookingEntity entity = entityManager.find(BookingEntity.class, booking.id());
        if (entity == null)
        {
            entity = new BookingEntity(booking.id(), booking.ownerType(), booking.ownerId(),
                    booking.startsAt(), booking.endsAt(), booking.status().name(), booking.createdAt());
            entityManager.persist(entity);
        }
        else
        {
            entity.setStatus(booking.status().name());
        }
        return toDomain(entity);
    }

    @Override
    public Optional<Booking> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(BookingEntity.class, id)).map(this::toDomain);
    }

    @Override
    public List<Booking> findByOwner(String ownerType, UUID ownerId)
    {
        return entityManager.createQuery(
                        "select b from BookingEntity b where b.ownerType = :ownerType and b.ownerId = :ownerId",
                        BookingEntity.class)
                .setParameter("ownerType", ownerType)
                .setParameter("ownerId", ownerId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    private Booking toDomain(BookingEntity entity)
    {
        return new Booking(entity.getId(), entity.getOwnerType(), entity.getOwnerId(),
                entity.getStartsAt(), entity.getEndsAt(),
                BookingStatus.valueOf(entity.getStatus()), entity.getCreatedAt());
    }
}
