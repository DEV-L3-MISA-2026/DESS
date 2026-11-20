package com.dess.assessment.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.ExamSession;

/**
 * Exam sessions of the assessment module.
 *
 * <p>Sessions are held in memory for now: the ExamSession model has no
 * persistence yet, and the persistence work lands with the exam
 * scheduling tasks. The list therefore starts empty on every start
 * and grows only through {@link #create(UUID, UUID, OffsetDateTime, OffsetDateTime)}.</p>
 */
public interface ExamSessionService
{
    ExamSession create(UUID assessmentId, UUID roomId,
                       OffsetDateTime startsAt, OffsetDateTime endsAt);

    Optional<ExamSession> findById(UUID id);

    List<ExamSession> findAll();
}
