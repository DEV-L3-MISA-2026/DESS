package com.dess.assessment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * @brief What is evaluated: an exam, a test or a practical evaluation.
 *
 * <p>An assessment is a definition, not an event. The event is the
 * exam session, owned by Christian, which takes place at one time and
 * in one room. One assessment can be held in several sessions.</p>
 *
 * <p>The assessment is associated with a course. The academic module
 * has no course offering yet, so the link is the course id, checked
 * through the academic application interface. When a course offering
 * lands, this link should move to the offering id.</p>
 *
 * <p>Rules of the model:</p>
 * <ul>
 *   <li>The title is required and has at most 200 characters.</li>
 *   <li>The maximum score is greater than zero and has at most two
 *       decimal places. It is stored as a decimal, never as a float,
 *       so that scores stay exact.</li>
 *   <li>The duration is a positive number of minutes. A missing,
 *       zero or negative duration is rejected. The unit is minutes,
 *       and nothing else: there is no second unit.</li>
 *   <li>Instants are stored in UTC. Any offset given by a caller is
 *       converted to UTC on construction.</li>
 *   <li>An assessment is never deleted. It is deactivated when it
 *       must no longer accept new results.</li>
 * </ul>
 */
public record Assessment(UUID id, UUID courseId, String title, AssessmentType type,
                         BigDecimal maximumScore, int durationMinutes, boolean active,
                         OffsetDateTime createdAt)
{
    public Assessment
    {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(courseId, "courseId is required");
        if (title == null || title.isBlank())
        {
            throw new IllegalArgumentException("title is required");
        }
        if (title.length() > 200)
        {
            throw new IllegalArgumentException("title must contain at most 200 characters");
        }
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(maximumScore, "maximumScore is required");
        if (maximumScore.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException("maximumScore must be greater than zero");
        }
        if (maximumScore.scale() > 2)
        {
            throw new IllegalArgumentException("maximumScore must have at most two decimal places");
        }
        if (maximumScore.compareTo(new BigDecimal("9999.99")) > 0)
        {
            throw new IllegalArgumentException("maximumScore must be at most 9999.99");
        }
        if (durationMinutes <= 0)
        {
            throw new IllegalArgumentException("durationMinutes must be greater than zero");
        }
        Objects.requireNonNull(createdAt, "createdAt is required");
        title = title.trim();
        createdAt = createdAt.withOffsetSameInstant(ZoneOffset.UTC);
    }

    /**
     * @brief Creates an active assessment with a new id and creation time.
     *
     * @param courseId the course this assessment evaluates
     * @param title the title shown to teachers and students
     * @param type the kind of evaluation
     * @param maximumScore the highest score a result can carry
     * @param durationMinutes the duration of the assessment, in minutes, greater than zero
     * @return the new assessment
     */
    public static Assessment create(UUID courseId, String title, AssessmentType type,
                                    BigDecimal maximumScore, int durationMinutes)
    {
        return new Assessment(UUID.randomUUID(), courseId, title, type, maximumScore,
                durationMinutes, true, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * @brief Returns a copy of this assessment that no longer accepts new results.
     *
     * <p>Existing results are kept. Only the active flag changes.</p>
     *
     * @return the deactivated assessment
     */
    public Assessment deactivated()
    {
        return new Assessment(id, courseId, title, type, maximumScore, durationMinutes,
                false, createdAt);
    }
}
