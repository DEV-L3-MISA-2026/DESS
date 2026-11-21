package com.dess.assessment.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dess.assessment.application.impl.ExamSessionServiceImpl;
import com.dess.assessment.application.spi.ExamSessionRepository;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.domain.ExamSessionConflictException;
import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.BookingStatus;
import com.dess.scheduling.domain.ResourceRef;

class ExamSessionServiceTest
{
    private FakeExamSessionRepository repository;
    private FakeBookingService bookingService;
    private ExamSessionServiceImpl service;

    @BeforeEach
    void setUp()
    {
        repository = new FakeExamSessionRepository();
        bookingService = new FakeBookingService();
        service = new ExamSessionServiceImpl(repository, bookingService);
    }

    @Test
    void startsEmpty()
    {
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void createsAndFindsSession()
    {
        ExamSession created = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00+03:00"), date("2026-11-23T11:00:00+03:00"));

        assertEquals(created, service.findById(created.id()).orElseThrow());
        assertEquals(1, service.findAll().size());
        assertEquals(ZoneOffset.UTC, created.startsAt().getOffset());
    }

    @Test
    void reservesRoomAndInvigilatorsWhenCreating()
    {
        UUID roomId = UUID.randomUUID();
        ExamSession created = service.create(UUID.randomUUID(), roomId,
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"), List.of(11L, 12L));

        Booking booking = bookingService.findByOwner(BookingService.EXAM_SESSION_OWNER,
                created.id()).get(0);
        assertEquals(BookingService.EXAM_SESSION_OWNER, booking.ownerType());
        assertEquals(BookingStatus.ACTIVE, booking.status());
        List<BookingSlot> slots = bookingService.findSlotsByBookingId(booking.id());
        assertEquals(3, slots.size());
        assertTrue(slots.stream().anyMatch(s -> s.resourceId().equals(roomId)));
        assertTrue(slots.stream().anyMatch(s -> s.resourceId().equals(ResourceRef.personId(11L).id())));
        assertTrue(slots.stream().anyMatch(s -> s.resourceId().equals(ResourceRef.personId(12L).id())));
    }

    @Test
    void rejectsRoomConflictAndDoesNotPersistSession()
    {
        UUID roomId = UUID.randomUUID();
        bookingService.create(BookingService.EXAM_SESSION_OWNER, UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"),
                List.of(new BookingService.ResourceRequest("ROOM", roomId)));

        assertThrows(ExamSessionConflictException.class,
                () -> service.create(UUID.randomUUID(), roomId,
                        date("2026-11-23T10:00:00Z"), date("2026-11-23T12:00:00Z")));

        assertTrue(service.findAll().isEmpty());
        assertEquals(1, bookingService.bookings.size());
    }

    @Test
    void rejectsInvigilatorConflictAndDoesNotPersistSession()
    {
        bookingService.create(BookingService.EXAM_SESSION_OWNER, UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"),
                List.of(new BookingService.ResourceRequest("PERSON",
                        ResourceRef.personId(11L).id())));

        assertThrows(ExamSessionConflictException.class,
                () -> service.create(UUID.randomUUID(), UUID.randomUUID(),
                        date("2026-11-23T10:00:00Z"), date("2026-11-23T12:00:00Z"),
                        List.of(11L)));

        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void acceptsAdjacentTimeRange()
    {
        UUID roomId = UUID.randomUUID();
        bookingService.create(BookingService.EXAM_SESSION_OWNER, UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"),
                List.of(new BookingService.ResourceRequest("ROOM", roomId)));

        ExamSession created = service.create(UUID.randomUUID(), roomId,
                date("2026-11-23T11:00:00Z"), date("2026-11-23T12:00:00Z"));

        assertEquals(created, service.findById(created.id()).orElseThrow());
    }

    @Test
    void replacesInvigilatorsAndTheirBooking()
    {
        ExamSession created = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"), List.of(11L));
        UUID oldBookingId = bookingService.findByOwner(BookingService.EXAM_SESSION_OWNER,
                created.id()).get(0).id();

        ExamSession updated = service.withInvigilators(created.id(), List.of(12L));

        assertEquals(List.of(12L), updated.invigilatorPersonIds());
        assertEquals(BookingStatus.CANCELLED, bookingService.bookings.get(oldBookingId).status());
        Booking newBooking = bookingService.findByOwner(BookingService.EXAM_SESSION_OWNER,
                created.id()).stream()
                .filter(b -> b.status() == BookingStatus.ACTIVE)
                .findFirst().orElseThrow();
        List<BookingSlot> slots = bookingService.findSlotsByBookingId(newBooking.id());
        assertEquals(2, slots.size());
        assertTrue(slots.stream().anyMatch(s -> s.resourceId().equals(ResourceRef.personId(12L).id())));
        assertFalse(slots.stream().anyMatch(s -> s.resourceId().equals(ResourceRef.personId(11L).id())));
    }

    @Test
    void linksCandidatesWithoutChangingTheBooking()
    {
        ExamSession created = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"));
        UUID bookingId = bookingService.findByOwner(BookingService.EXAM_SESSION_OWNER,
                created.id()).get(0).id();

        ExamSession updated = service.withCandidates(created.id(), List.of(21L, 22L));

        assertEquals(List.of(21L, 22L), updated.candidatePersonIds());
        assertEquals(BookingStatus.ACTIVE, bookingService.bookings.get(bookingId).status());
    }

    @Test
    void cancelReleasesBookingAndRemovesSession()
    {
        ExamSession created = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"), List.of(11L));

        service.cancel(created.id());

        assertTrue(service.findById(created.id()).isEmpty());
        Booking booking = bookingService.findByOwner(BookingService.EXAM_SESSION_OWNER,
                created.id()).get(0);
        assertEquals(BookingStatus.CANCELLED, booking.status());
        assertTrue(bookingService.findSlotsByBookingId(booking.id()).stream()
                .allMatch(s -> s.status() == BookingStatus.CANCELLED));
    }

    @Test
    void conflictMessageNamesTheResourceTimeRangeAndOwner()
    {
        UUID roomId = UUID.randomUUID();
        bookingService.create(BookingService.TEACHING_SESSION_OWNER, UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"),
                List.of(new BookingService.ResourceRequest("ROOM", roomId)));

        ExamSessionConflictException exception = assertThrows(ExamSessionConflictException.class,
                () -> service.create(UUID.randomUUID(), roomId,
                        date("2026-11-23T10:00:00Z"), date("2026-11-23T12:00:00Z")));

        assertEquals("the room is already booked from 2026-11-23 09:00 to 2026-11-23 11:00 UTC by SESSION.",
                exception.getMessage());
    }

    @Test
    void rejectsUnknownSessionForInvigilatorUpdate()
    {
        assertThrows(IllegalArgumentException.class,
                () -> service.withInvigilators(UUID.randomUUID(), List.of(11L)));
    }

    @Test
    void listsNewestFirst()
    {
        service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00Z"), date("2026-11-23T11:00:00Z"));
        ExamSession later = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-24T09:00:00Z"), date("2026-11-24T11:00:00Z"));

        List<ExamSession> sessions = service.findAll();

        assertEquals(2, sessions.size());
        assertEquals(later, sessions.get(0));
    }

    @Test
    void returnsEmptyForUnknownId()
    {
        assertTrue(service.findById(UUID.randomUUID()).isEmpty());
    }

    private static OffsetDateTime date(String text)
    {
        return OffsetDateTime.parse(text);
    }

    private static class FakeExamSessionRepository implements ExamSessionRepository
    {
        private final Map<UUID, ExamSession> sessions = new LinkedHashMap<>();

        @Override
        public ExamSession save(ExamSession session)
        {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<ExamSession> findById(UUID id)
        {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public List<ExamSession> findAll()
        {
            return sessions.values().stream()
                    .sorted(Comparator.comparing(ExamSession::startsAt).reversed())
                    .toList();
        }

        @Override
        public void delete(UUID id)
        {
            sessions.remove(id);
        }
    }

    private static class FakeBookingService implements BookingService
    {
        private final Map<UUID, Booking> bookings = new LinkedHashMap<>();
        private final Map<UUID, List<BookingSlot>> slots = new LinkedHashMap<>();

        @Override
        public Booking create(String ownerType, UUID ownerId, OffsetDateTime startsAt,
                              OffsetDateTime endsAt, List<ResourceRequest> resources)
        {
            Booking booking = Booking.create(ownerType, ownerId, startsAt, endsAt);
            List<BookingSlot> created = new ArrayList<>();

            for (ResourceRequest resource : resources)
            {
                if (!isAvailable(resource.resourceType(), resource.resourceId(), startsAt, endsAt))
                {
                    throw new IllegalArgumentException("resource is already booked for this time range");
                }
                created.add(BookingSlot.create(booking.id(), resource.resourceType(),
                        resource.resourceId(), startsAt, endsAt));
            }

            bookings.put(booking.id(), booking);
            slots.put(booking.id(), created);
            return booking;
        }

        @Override
        public Booking cancel(UUID bookingId)
        {
            Booking booking = bookings.get(bookingId);
            if (booking == null)
            {
                throw new IllegalArgumentException("booking not found: " + bookingId);
            }
            Booking cancelled = booking.cancel();
            bookings.put(bookingId, cancelled);
            slots.put(bookingId, slots.getOrDefault(bookingId, List.of()).stream()
                    .map(BookingSlot::cancel).toList());
            return cancelled;
        }

        @Override
        public Optional<Booking> findById(UUID id)
        {
            return Optional.ofNullable(bookings.get(id));
        }

        @Override
        public List<Booking> findAll()
        {
            return List.copyOf(bookings.values());
        }

        @Override
        public List<Booking> findByOwner(String ownerType, UUID ownerId)
        {
            return bookings.values().stream()
                    .filter(b -> b.ownerType().equals(ownerType) && b.ownerId().equals(ownerId))
                    .toList();
        }

        @Override
        public List<BookingSlot> findSlotsByBookingId(UUID bookingId)
        {
            return slots.getOrDefault(bookingId, List.of());
        }

        @Override
        public boolean isAvailable(String resourceType, UUID resourceId,
                                   OffsetDateTime startsAt, OffsetDateTime endsAt)
        {
            return slots.values().stream().flatMap(List::stream)
                    .filter(s -> s.status() == BookingStatus.ACTIVE)
                    .filter(s -> s.resourceType().equals(resourceType))
                    .filter(s -> s.resourceId().equals(resourceId))
                    .noneMatch(s -> s.startsAt().isBefore(endsAt) && s.endsAt().isAfter(startsAt));
        }
    }
}
