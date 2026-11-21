package com.dess.assessment.domain;

/**
 * @brief The kind of an assessment.
 *
 * <p>From the architecture summary: an assessment is an evaluation
 * associated with a course offering, such as an exam, a test or a
 * practical evaluation.</p>
 */
public enum AssessmentType
{
    /** A formal examination, usually held in an exam session. */
    EXAM,
    /** A shorter test, usually held during teaching time. */
    TEST,
    /** A practical evaluation, such as a lab or a workshop task. */
    PRACTICAL
}
