package com.dess.assessment.application;

import java.util.List;
import java.util.UUID;

/**
 * @brief Application API of the candidate eligibility check.
 *
 * <p>Eligibility answers one question: may this student sit this
 * assessment? The rules come from the plan and the architecture
 * summary: eligible students are found from the enrollment rules.
 * On this branch the enrollment rule that exists is the program
 * enrollment, because the academic module has no course offering
 * yet. When a course offering lands, the check should also require
 * an enrollment for the offering of the assessment.</p>
 */
public interface EligibilityService
{
    /**
     * @brief The outcome of one eligibility check.
     *
     * @param eligible true when the student may sit the assessment
     * @param reasons why the student is not eligible, as stable
     *                codes; empty when eligible. The user-facing
     *                sentence for a code lives in
     *                {@code EligibilityPresentation}.
     */
    record Eligibility(boolean eligible, List<EligibilityReason> reasons)
    {
    }

    Eligibility check(UUID assessmentId, long studentPersonId);

    List<Long> eligibleStudents(UUID assessmentId, List<Long> studentPersonIds);
}
