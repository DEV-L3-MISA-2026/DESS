package com.dess.assessment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * @brief One student's result for one assessment.
 *
 * <p>A result belongs to an assessment and to a student. It records
 * which exam session the student sat, when the session id is known.
 * The session id is optional on purpose: exam sessions have no
 * persistence yet on the dev branch, so a result must be recordable
 * without a stored session. When session persistence lands, the
 * result service should check that the session exists, belongs to
 * the assessment and lists the student as a candidate.</p>
 *
 * <p>One student has at most one result per assessment. That is the
 * MVP rule: a retake is a new assessment, not a second result. The
 * database enforces it with a unique constraint on
 * (assessment, student), and the service checks it before saving.</p>
 *
 * <p>Rules of the model:</p>
 * <ul>
 *   <li>A SCORED result carries a score. An ABSENT or EXCUSED
 *       result carries no score.</li>
 *   <li>A score is at least zero and has at most two decimal places.
 *       The upper bound is the assessment maximum score, checked by
 *       the result service, which knows the assessment.</li>
 *   <li>The student is a person id, the numeric id of the people
 *       module, as in the exam session model.</li>
 *   <li>The audit fields are kept: who entered the result, when it
 *       was entered and when it was last corrected. A correction
 *       never changes who entered it or when it was entered.</li>
 *   <li>Instants are stored in UTC. Any offset given by a caller is
 *       converted to UTC on construction.</li>
 * </ul>
 */
public record Result(UUID id, UUID assessmentId, UUID examSessionId, long studentPersonId,
                     BigDecimal score, ResultStatus status, String enteredBy,
                     OffsetDateTime enteredAt, OffsetDateTime updatedAt)
{
    public Result
    {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(assessmentId, "assessmentId is required");
        if (studentPersonId <= 0)
        {
            throw new IllegalArgumentException("studentPersonId must be greater than zero");
        }
        Objects.requireNonNull(status, "status is required");
        if (status == ResultStatus.SCORED)
        {
            if (score == null)
            {
                throw new IllegalArgumentException("score is required for a SCORED result");
            }
            if (score.compareTo(BigDecimal.ZERO) < 0)
            {
                throw new IllegalArgumentException("score must be at least zero");
            }
            if (score.scale() > 2)
            {
                throw new IllegalArgumentException("score must have at most two decimal places");
            }
        }
        else if (score != null)
        {
            throw new IllegalArgumentException("score must be empty for an ABSENT or EXCUSED result");
        }
        if (enteredBy == null || enteredBy.isBlank())
        {
            throw new IllegalArgumentException("enteredBy is required");
        }
        if (enteredBy.length() > 200)
        {
            throw new IllegalArgumentException("enteredBy must contain at most 200 characters");
        }
        Objects.requireNonNull(enteredAt, "enteredAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
        enteredBy = enteredBy.trim();
        enteredAt = enteredAt.withOffsetSameInstant(ZoneOffset.UTC);
        updatedAt = updatedAt.withOffsetSameInstant(ZoneOffset.UTC);
    }

    /**
     * @brief Creates a result entered now.
     *
     * @param assessmentId the assessment this result belongs to
     * @param examSessionId the session the student sat, or null when it is not known
     * @param studentPersonId the numeric person id of the student
     * @param score the score, or null for an ABSENT or EXCUSED result
     * @param status the outcome of the attempt
     * @param enteredBy who entered the result
     * @return the new result
     */
    public static Result create(UUID assessmentId, UUID examSessionId, long studentPersonId,
                                BigDecimal score, ResultStatus status, String enteredBy)
    {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Result(UUID.randomUUID(), assessmentId, examSessionId, studentPersonId,
                score, status, enteredBy, now, now);
    }

    /**
     * @brief Returns a copy of this result with a corrected score and status.
     *
     * <p>Only the score, the status and the update time change. The
     * person who entered the result and the entry time are kept, so
     * the audit trail is not rewritten by a correction.</p>
     *
     * @param score the corrected score, or null for an ABSENT or EXCUSED result
     * @param status the corrected outcome
     * @return the corrected result
     */
    public Result corrected(BigDecimal score, ResultStatus status)
    {
        return new Result(id, assessmentId, examSessionId, studentPersonId,
                score, status, enteredBy, enteredAt, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
