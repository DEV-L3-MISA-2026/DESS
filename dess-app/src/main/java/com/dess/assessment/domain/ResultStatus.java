package com.dess.assessment.domain;

/**
 * @brief The status of a result.
 *
 * <p>From the MVP plan: basic results carry a status and audit fields.
 * Grade averages, weights, retakes and ranking are out of the MVP
 * scope, so this type records only the outcome of one attempt.</p>
 */
public enum ResultStatus
{
    /** A score was entered for the student. */
    SCORED,
    /** The student was absent from the exam session. No score. */
    ABSENT,
    /** The student was excused from the exam session. No score. */
    EXCUSED
}
