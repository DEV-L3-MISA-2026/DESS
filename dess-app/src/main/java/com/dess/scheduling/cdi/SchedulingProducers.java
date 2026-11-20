package com.dess.scheduling.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.RoomService;
import com.dess.scheduling.application.SchedulingService;
import com.dess.scheduling.application.impl.BookingServiceImpl;
import com.dess.scheduling.application.impl.RoomServiceImpl;
import com.dess.scheduling.application.impl.SchedulingServiceImpl;
import com.dess.scheduling.infrastructure.persistence.JpaBookingRepository;
import com.dess.scheduling.infrastructure.persistence.JpaBookingSlotRepository;
import com.dess.scheduling.infrastructure.persistence.JpaRoomRepository;

@ApplicationScoped
public class SchedulingProducers
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Produces
    @ApplicationScoped
    public RoomService roomService()
    {
        return new RoomServiceImpl(new JpaRoomRepository(entityManager));
    }

    @Produces
    @ApplicationScoped
    public BookingService bookingService()
    {
        return new BookingServiceImpl(
                new JpaBookingRepository(entityManager),
                new JpaBookingSlotRepository(entityManager),
                new JpaRoomRepository(entityManager));
    }

    @Produces
    @ApplicationScoped
    public SchedulingService schedulingService(RoomService roomService, BookingService bookingService)
    {
        return new SchedulingServiceImpl(roomService, bookingService);
    }
}
