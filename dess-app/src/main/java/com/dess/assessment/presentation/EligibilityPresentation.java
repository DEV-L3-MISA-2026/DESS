package com.dess.assessment.presentation;

import java.util.Objects;
import java.util.stream.Collectors;

import com.dess.assessment.application.EligibilityReason;
import com.dess.assessment.application.EligibilityService;

/**
 * @brief The one English wording of each eligibility reason.
 *
 * <p>The eligibility service returns stable reason codes, never
 * sentences. This presenter is the single place that turns a code
 * into the sentence a user reads. The page (issue #33) and the API
 * error path (the message of {@code IneligibleCandidateException},
 * built by the result service) both use it, so they can never drift
 * apart.</p>
 *
 * <p>The presenter lives in the assessment module, not in the web
 * layer, on purpose: the result service needs the same wording for
 * its exception message, and the application layer must not depend
 * on the web layer. Pages still use this presenter; they must not
 * keep their own copy of a sentence.</p>
 *
 * <p>Only English sentences live here. The interface language is
 * English, and no French reason is rendered anywhere in the
 * assessment module.</p>
 */
public final class EligibilityPresentation
{
    private EligibilityPresentation()
    {
    }

    /**
     * @brief Returns the English sentence for one reason code.
     *
     * @param reason the stable code
     * @param studentPersonId the student the check ran for; only
     *                        used in the sentence for
     *                        {@code STUDENT_NOT_FOUND}
     * @return the sentence shown to the user
     */
    public static String reasonMessage(EligibilityReason reason, long studentPersonId)
    {
        Objects.requireNonNull(reason, "reason is required");
        return switch (reason)
        {
            case ASSESSMENT_INACTIVE -> "assessment is not active";
            case STUDENT_NOT_FOUND -> "student not found: " + studentPersonId;
            case NO_ACTIVE_PROGRAM_ENROLLMENT -> "student has no active program enrollment";
            case NO_OFFERING_ENROLLMENT -> "student has no enrollment for the offering";
        };
    }

    /**
     * @brief Returns the sentences for all reasons of one check.
     *
     * @param eligibility the outcome of the check
     * @param studentPersonId the student the check ran for
     * @return the sentences joined with "; ", or an empty string
     *         when the student is eligible
     */
    public static String reasonMessages(EligibilityService.Eligibility eligibility,
                                        long studentPersonId)
    {
        Objects.requireNonNull(eligibility, "eligibility is required");
        return eligibility.reasons().stream()
                .map(reason -> reasonMessage(reason, studentPersonId))
                .collect(Collectors.joining("; "));
    }
}
