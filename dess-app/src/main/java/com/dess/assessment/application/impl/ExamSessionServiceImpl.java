package com.dess.assessment.application.impl;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import com.dess.assessment.application.ExamSessionService;
import com.dess.assessment.domain.ExamSession;

/**
 * In-memory exam sessions.
 *
 * <p>There is no repository behind this service yet: ExamSession has no
 * persistence, by design of the model task. Sessions live for the
 * lifetime of the application and are listed newest first, like the
 * bookings on the scheduling page.</p>
 */
@ApplicationScoped
public class ExamSessionServiceImpl implements ExamSessionService
{
    private final List<ExamSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public ExamSession create(UUID assessmentId, UUID roomId,
                              OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        ExamSession session = ExamSession.create(assessmentId, roomId, startsAt, endsAt);
        sessions.add(session);
        return session;
    }

    @Override
    public Optional<ExamSession> findById(UUID id)
    {
        return sessions.stream()
                .filter(session -> session.id().equals(id))
                .findFirst();
    }

    @Override
    public List<ExamSession> findAll()
    {
        return sessions.stream()
                .sorted(Comparator.comparing(ExamSession::startsAt).reversed()
                        .thenComparing(Comparator.comparing(ExamSession::createdAt).reversed()))
                .toList();
    }
}
