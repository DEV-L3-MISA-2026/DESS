package com.dess.assessment.application.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.dess.assessment.application.ExamSessionService;
import com.dess.assessment.application.spi.ExamSessionRepository;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.domain.ExamSessionConflictException;
import com.dess.scheduling.application.BookingRequest;
import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.domain.Booking;
import com.dess.scheduling.domain.BookingSlot;
import com.dess.scheduling.domain.BookingStatus;
import com.dess.scheduling.domain.ResourceRef;

/**
 * @brief Application service of the assessment module.
 *
 * <p>The class is {@code @Transactional}: a rejected booking rolls back both the
 * booking write and the session write, so a conflict never leaves a confirmed session.</p>
 */
@ApplicationScoped
@Transactional
public class ExamSessionServiceImpl implements ExamSessionService
{
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ExamSessionRepository repository;
    private final BookingService bookingService;

    @Inject
    public ExamSessionServiceImpl(ExamSessionRepository repository,
                                  BookingService bookingService)
    {
        this.repository = repository;
        this.bookingService = bookingService;
    }

    @Override
    public ExamSession create(UUID assessmentId, UUID roomId,
                              OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        return create(assessmentId, roomId, startsAt, endsAt, List.of());
    }

    @Override
    public ExamSession create(UUID assessmentId, UUID roomId,
                              OffsetDateTime startsAt, OffsetDateTime endsAt,
                              List<Long> invigilatorPersonIds)
    {
        ExamSession session = ExamSession.create(assessmentId, roomId, startsAt, endsAt);
        reserve(session, invigilatorPersonIds);
        return repository.save(session);
    }

    @Override
    public ExamSession withInvigilators(UUID sessionId, List<Long> invigilatorPersonIds)
    {
        ExamSession updated = find(sessionId).withInvigilators(invigilatorPersonIds);
        cancelActiveBooking(sessionId);
        reserve(updated, updated.invigilatorPersonIds());
        return repository.save(updated);
    }

    @Override
    public ExamSession withCandidates(UUID sessionId, List<Long> candidatePersonIds)
    {
        ExamSession updated = find(sessionId).withCandidates(candidatePersonIds);
        return repository.save(updated);
    }

    @Override
    public void cancel(UUID sessionId)
    {
        find(sessionId);
        cancelActiveBooking(sessionId);
        repository.delete(sessionId);
    }

    @Override
    public Optional<ExamSession> findById(UUID id)
    {
        return repository.findById(id);
    }

    @Override
    public List<ExamSession> findAll()
    {
        return repository.findAll();
    }

    private ExamSession find(UUID sessionId)
    {
        return repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("exam session not found: " + sessionId));
    }

    private void reserve(ExamSession session, List<Long> invigilatorPersonIds)
    {
        List<ResourceRef> resources = new ArrayList<>();
        resources.add(ResourceRef.room(session.roomId()));
        for (Long personId : invigilatorPersonIds)
        {
            resources.add(ResourceRef.personId(personId));
        }

        for (ResourceRef resource : resources)
        {
            Booking conflict = findConflict(resource, session.startsAt(), session.endsAt());

            if (conflict != null)
            {
                throw new ExamSessionConflictException(
                        describe(resource) + " is already booked from "
                        + MOMENT.format(conflict.startsAt().withOffsetSameInstant(ZoneOffset.UTC))
                        + " to " + MOMENT.format(conflict.endsAt().withOffsetSameInstant(ZoneOffset.UTC))
                        + " UTC by " + conflict.ownerType() + ".");
            }
        }

        BookingRequest request = new BookingRequest(BookingService.EXAM_SESSION_OWNER,
                session.id(), session.startsAt(), session.endsAt(), resources);
        bookingService.create(request);
    }

    private void cancelActiveBooking(UUID sessionId)
    {
        for (Booking booking : bookingService.findByOwner(BookingService.EXAM_SESSION_OWNER, sessionId))
        {
            if (booking.status() == BookingStatus.ACTIVE)
            {
                bookingService.cancel(booking.id());
            }
        }
    }

    private Booking findConflict(ResourceRef resource, OffsetDateTime startsAt,
                                 OffsetDateTime endsAt)
    {
        for (Booking booking : bookingService.findAll())
        {
            if (booking.status() != BookingStatus.ACTIVE)
            {
                continue;
            }
            for (BookingSlot slot : bookingService.findSlotsByBookingId(booking.id()))
            {
                if (slot.status() == BookingStatus.ACTIVE
                        && slot.resourceType().equals(resource.typeName())
                        && slot.resourceId().equals(resource.id())
                        && slot.startsAt().isBefore(endsAt)
                        && slot.endsAt().isAfter(startsAt))
                {
                    return booking;
                }
            }
        }
        return null;
    }

    private String describe(ResourceRef resource)
    {
        return switch (resource.type())
        {
            case ROOM -> "the room";
            case PERSON -> "an invigilator";
            case STUDENT_GROUP -> "the student group";
        };
    }
}
