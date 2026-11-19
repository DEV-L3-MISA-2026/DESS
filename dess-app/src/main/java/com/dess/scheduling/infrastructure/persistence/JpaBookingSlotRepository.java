package com.dess.scheduling.infrastructure.persistence;

import com.dess.scheduling.application.spi.BookingSlotRepository;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.BookingStatus;
import com.dess.scheduling.infrastructure.persistence.entity.BookingEntity;
import com.dess.scheduling.infrastructure.persistence.entity.BookingSlotEntity;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaBookingSlotRepository implements BookingSlotRepository
{
    private final EntityManager entityManager;

    public JpaBookingSlotRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public BookingSlot save(BookingSlot slot)
    {
        BookingSlotEntity entity = entityManager.find(BookingSlotEntity.class, slot.id());
        if (entity == null)
        {
            BookingEntity booking = entityManager.find(BookingEntity.class, slot.bookingId());
            if (booking == null)
            {
                throw new IllegalArgumentException("booking does not exist: " + slot.bookingId());
            }
            entity = new BookingSlotEntity(slot.id(), booking, slot.resourceType(), slot.resourceId(),
                    slot.startsAt(), slot.endsAt(), slot.status().name());
            entityManager.persist(entity);
        }
        else
        {
            entity.setStatus(slot.status().name());
        }
        return toDomain(entity);
    }

    @Override
    public Optional<BookingSlot> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(BookingSlotEntity.class, id)).map(this::toDomain);
    }

    @Override
    public List<BookingSlot> findByBookingId(UUID bookingId)
    {
        return entityManager.createQuery(
                        "select s from BookingSlotEntity s where s.booking.id = :bookingId",
                        BookingSlotEntity.class)
                .setParameter("bookingId", bookingId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean hasOverlap(String resourceType, UUID resourceId,
                              OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        Long count = entityManager.createQuery(
                        "select count(s) from BookingSlotEntity s "
                                + "where s.resourceType = :resourceType "
                                + "and s.resourceId = :resourceId "
                                + "and s.status = 'ACTIVE' "
                                + "and s.startsAt < :endsAt and s.endsAt > :startsAt",
                        Long.class)
                .setParameter("resourceType", resourceType)
                .setParameter("resourceId", resourceId)
                .setParameter("startsAt", startsAt)
                .setParameter("endsAt", endsAt)
                .getSingleResult();
        return count != null && count > 0;
    }

    private BookingSlot toDomain(BookingSlotEntity entity)
    {
        return new BookingSlot(entity.getId(), entity.getBooking().getId(), entity.getResourceType(),
                entity.getResourceId(), entity.getStartsAt(), entity.getEndsAt(),
                BookingStatus.valueOf(entity.getStatus()));
    }
}
