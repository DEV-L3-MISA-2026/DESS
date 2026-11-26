package com.dess.assessment.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.dess.assessment.application.EligibilityReason;
import com.dess.assessment.application.EligibilityService;

class EligibilityPresentationTest
{
    @Test
    void inactiveAssessmentHasItsOwnSentence()
    {
        assertEquals("assessment is not active",
                EligibilityPresentation.reasonMessage(
                        EligibilityReason.ASSESSMENT_INACTIVE, 7L));
    }

    @Test
    void unknownStudentSentenceNamesTheStudent()
    {
        assertEquals("student not found: 7",
                EligibilityPresentation.reasonMessage(
                        EligibilityReason.STUDENT_NOT_FOUND, 7L));
    }

    @Test
    void missingEnrollmentHasItsOwnSentence()
    {
        assertEquals("student has no active program enrollment",
                EligibilityPresentation.reasonMessage(
                        EligibilityReason.NO_ACTIVE_PROGRAM_ENROLLMENT, 7L));
    }

    @Test
    void offeringReasonHasItsOwnSentence()
    {
        assertEquals("student has no enrollment for the offering",
                EligibilityPresentation.reasonMessage(
                        EligibilityReason.NO_OFFERING_ENROLLMENT, 7L));
    }

    @Test
    void sentencesJoinInCodeOrder()
    {
        EligibilityService.Eligibility eligibility = new EligibilityService.Eligibility(false,
                List.of(EligibilityReason.ASSESSMENT_INACTIVE,
                        EligibilityReason.STUDENT_NOT_FOUND));

        assertEquals("assessment is not active; student not found: 7",
                EligibilityPresentation.reasonMessages(eligibility, 7L));
    }

    @Test
    void eligibleCheckHasNoSentences()
    {
        EligibilityService.Eligibility eligibility =
                new EligibilityService.Eligibility(true, List.of());

        assertEquals("", EligibilityPresentation.reasonMessages(eligibility, 7L));
    }

    @Test
    void sentencesAreEnglishOnly()
    {
        for (EligibilityReason reason : EligibilityReason.values())
        {
            String sentence = EligibilityPresentation.reasonMessage(reason, 7L);
            assertTrue(sentence.chars().allMatch(character -> character < 128),
                    "sentence for " + reason + " must be plain English, without accents: "
                            + sentence);
        }
    }

    @Test
    void nullReasonIsRejected()
    {
        assertThrows(NullPointerException.class,
                () -> EligibilityPresentation.reasonMessage(null, 7L));
    }
}
