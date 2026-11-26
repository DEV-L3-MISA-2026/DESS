package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dess.assessment.application.EligibilityReason;

class EligibilityViewTest
{
    @Test
    void countLabelCountsEligibleOfTotal()
    {
        EligibilityView view = view(List.of(eligible(1L)), List.of(ineligible(2L)));

        assertEquals("1 eligible candidate of 2 candidates. Candidates from 1 exam session.",
                view.getCountLabel());
    }

    @Test
    void eligibleAndIneligibleCandidatesAreSplit()
    {
        EligibilityView view = view(List.of(eligible(1L), eligible(3L)),
                List.of(ineligible(2L)));

        assertEquals(2, view.getEligibleCandidates().size());
        assertEquals(1, view.getIneligibleCandidates().size());
        assertEquals("Eligible", view.getEligibleCandidates().get(0).getEligibility().getLabel());
        assertEquals("Not eligible",
                view.getIneligibleCandidates().get(0).getEligibility().getLabel());
        assertEquals("student not found: 2",
                view.getIneligibleCandidates().get(0).getReasonMessage());
    }

    @Test
    void noCandidatesNamesTheNextAction()
    {
        EligibilityView view = view(List.of(), List.of());

        assertEquals("No candidates yet.", view.getCandidatesEmpty().getTitle());
        assertEquals("Manage exam sessions", view.getCandidatesEmpty().getActionLabel());
        assertEquals("/app/exam-sessions", view.getCandidatesEmpty().getActionHref());
    }

    @Test
    void allEligibleExplainsTheEmptyIneligibleList()
    {
        EligibilityView view = view(List.of(eligible(1L)), List.of());

        assertEquals("No ineligible candidate.", view.getIneligibleEmpty().getTitle());
        assertEquals("Every candidate checked may sit this assessment.",
                view.getIneligibleEmpty().getMessage());
    }

    @Test
    void noneEligibleExplainsTheEmptyEligibleList()
    {
        EligibilityView view = view(List.of(), List.of(ineligible(2L)));

        assertEquals("No eligible candidate.", view.getEligibleEmpty().getTitle());
    }

    @Test
    void contextKeepsTheAssessmentDetails()
    {
        EligibilityView view = view(List.of(), List.of());

        assertEquals("Final exam", view.getAssessmentTitle());
        assertEquals("MATH101 - Mathematics", view.getCourse());
        assertEquals("Exam", view.getType());
        assertEquals("20", view.getMaximumScore());
        assertEquals(90, view.getDurationMinutes());
        assertEquals("Active", view.getStatus().getLabel());
    }

    private static EligibilityView view(List<CandidateRow> eligible,
                                        List<CandidateRow> ineligible)
    {
        return new EligibilityView("Final exam", "MATH101 - Mathematics", "Exam", "20", 90,
                Badge.active(true), "Candidates from 1 exam session", eligible, ineligible,
                "", Map.of());
    }

    private static CandidateRow eligible(long personId)
    {
        return new CandidateRow(personId, "Ada Lovelace", "STU-001", true, List.of(), "");
    }

    private static CandidateRow ineligible(long personId)
    {
        return new CandidateRow(personId, "Unknown person", "", false,
                List.of(EligibilityReason.STUDENT_NOT_FOUND), "student not found: " + personId);
    }
}
