package com.dess.assessment.application;

/**
 * @brief Stable codes for why a student is not eligible for an assessment.
 *
 * <p>The code is the stable contract: tests assert the code, never
 * the sentence. The English sentence shown to a user lives in
 * {@code EligibilityPresentation} and is rendered from the code, so
 * the page and the API error always use the same wording.</p>
 */
public enum EligibilityReason
{
    /** The assessment exists but is deactivated. */
    ASSESSMENT_INACTIVE,

    /** No student exists with that person id. */
    STUDENT_NOT_FOUND,

    /** The student exists but has no ACTIVE program enrollment. */
    NO_ACTIVE_PROGRAM_ENROLLMENT,

    /**
     * The student has no enrollment for the offering of the
     * assessment. Placeholder for issue #111: it is never returned
     * until issue #18 (course offerings) lands, because there is no
     * offering enrollment to check yet. It exists now so the code is
     * stable when the check is added.
     */
    NO_OFFERING_ENROLLMENT
}
