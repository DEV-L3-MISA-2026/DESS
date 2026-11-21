package com.dess.assessment.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.ExamSession;

/**
 * @brief Exam sessions of the assessment module.
 *
 * <p>Creating a session also reserves its room and invigilators through the scheduling
 * booking service. A conflict leaves no session and no booking behind. Sessions are
 * persisted through the assessment repository port; nothing is kept only in memory.</p>
 */
public interface ExamSessionService
{
    /**
     * @brief Creates a session without invigilators.
     * @param assessmentId assessment definition id
     * @param roomId room id
     * @param startsAt start instant, converted to UTC
     * @param endsAt end instant, converted to UTC
     * @return the persisted session
     */
    ExamSession create(UUID assessmentId, UUID roomId,
                       OffsetDateTime startsAt, OffsetDateTime endsAt);

    /**
     * @brief Creates a session and reserves its room and invigilators.
     * @param assessmentId assessment definition id
     * @param roomId room id
     * @param startsAt start instant, converted to UTC
     * @param endsAt end instant, converted to UTC
     * @param invigilatorPersonIds person ids of the invigilators
     * @return the persisted session
     */
    ExamSession create(UUID assessmentId, UUID roomId,
                       OffsetDateTime startsAt, OffsetDateTime endsAt,
                       List<Long> invigilatorPersonIds);

    /**
     * @brief Replaces the invigilators and their reservations.
     * @param sessionId session id
     * @param invigilatorPersonIds new person ids
     * @return the updated session
     */
    ExamSession withInvigilators(UUID sessionId, List<Long> invigilatorPersonIds);

    /**
     * @brief Links candidates once eligibility is known.
     * @param sessionId session id
     * @param candidatePersonIds new person ids
     * @return the updated session
     */
    ExamSession withCandidates(UUID sessionId, List<Long> candidatePersonIds);

    /**
     * @brief Releases the booking and removes the session.
     * @param sessionId session id
     */
    void cancel(UUID sessionId);

    /**
     * @param id session id
     * @return the stored session, or an empty result
     */
    Optional<ExamSession> findById(UUID id);

    /**
     * @return all stored sessions, newest first
     */
    List<ExamSession> findAll();
}
