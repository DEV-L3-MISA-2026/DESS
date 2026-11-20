package com.dess.scheduling.cdi;

import com.dess.scheduling.application.spi.BookingRepository;
import com.dess.scheduling.application.spi.BookingSlotRepository;
import com.dess.scheduling.application.spi.RoomRepository;
import com.dess.scheduling.infrastructure.persistence.JpaBookingRepository;
import com.dess.scheduling.infrastructure.persistence.JpaBookingSlotRepository;
import com.dess.scheduling.infrastructure.persistence.JpaRoomRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * CDI producers of the scheduling module.
 *
 * <p>Only repositories are produced. The service implementations are ordinary beans so
 * their {@code @Transactional} annotation is applied by the container; a bean returned
 * from a producer method is not intercepted.</p>
 */
@ApplicationScoped
public class SchedulingProducers
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Produces
    @ApplicationScoped
    public RoomRepository roomRepository()
    {
        return new JpaRoomRepository(entityManager);
    }

    @Produces
    @ApplicationScoped
    public BookingRepository bookingRepository()
    {
        return new JpaBookingRepository(entityManager);
    }

    @Produces
    @ApplicationScoped
    public BookingSlotRepository bookingSlotRepository()
    {
        return new JpaBookingSlotRepository(entityManager);
    }
}
