package com.dess.platform.web.view;

import java.util.List;
import java.util.Objects;

import com.dess.assessment.application.EligibilityReason;

/**
 * One candidate on the eligibility page.
 *
 * <p>The row carries exactly what the eligibility service returned: whether
 * the candidate may sit the assessment, and the stable reason codes when
 * not. The sentence shown to the user comes from
 * {@code EligibilityPresentation} and is never written here.</p>
 */
public final class CandidateRow
{
    private final long personId;
    private final String name;
    private final String registrationNumber;
    private final boolean eligible;
    private final List<EligibilityReason> reasons;
    private final String reasonMessage;
    private final Badge eligibility;

    /**
     * @param personId numeric person id of the candidate
     * @param name candidate name, or {@code Unknown person} when no person exists
     * @param registrationNumber student registration number, or an empty string
     *                           when the person is not a known student
     * @param eligible whether the service says the candidate may sit the assessment
     * @param reasons stable reason codes; empty when eligible
     * @param reasonMessage the English sentence for the reasons, or an empty
     *                      string when eligible
     */
    public CandidateRow(long personId, String name, String registrationNumber,
                        boolean eligible, List<EligibilityReason> reasons,
                        String reasonMessage)
    {
        this.personId = personId;
        this.name = Objects.requireNonNull(name, "name is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber,
                "registrationNumber is required");
        this.eligible = eligible;
        this.reasons = List.copyOf(reasons);
        this.reasonMessage = Objects.requireNonNull(reasonMessage, "reasonMessage is required");
        this.eligibility = Badge.eligible(eligible);
    }

    public long getPersonId()
    {
        return personId;
    }

    public String getName()
    {
        return name;
    }

    public String getRegistrationNumber()
    {
        return registrationNumber;
    }

    public boolean isEligible()
    {
        return eligible;
    }

    public List<EligibilityReason> getReasons()
    {
        return reasons;
    }

    public String getReasonMessage()
    {
        return reasonMessage;
    }

    public Badge getEligibility()
    {
        return eligibility;
    }
}
