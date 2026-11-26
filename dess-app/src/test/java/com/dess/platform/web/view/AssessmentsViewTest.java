package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentsViewTest
{
    @Test
    void countLabelCountsTheVisibleAssessments()
    {
        AssessmentsView view = view(List.of(row("Final exam")), "", "", true);

        assertEquals("1 assessment shown. Duration is in minutes.", view.getCountLabel());
    }

    @Test
    void anEmptyFirstVisitNamesTheNextActionForAWriter()
    {
        AssessmentsView view = view(List.of(), "", "", true);

        assertEquals("No assessment yet.", view.getAssessmentsEmpty().getTitle());
        assertEquals("Create the first assessment below.",
                view.getAssessmentsEmpty().getMessage());
    }

    @Test
    void anEmptyFirstVisitHasNoCreateActionForAReader()
    {
        AssessmentsView view = view(List.of(), "", "", false);

        assertEquals("No assessment definition has been created yet.",
                view.getAssessmentsEmpty().getMessage());
        assertFalse(view.getChrome().isWritable());
    }

    @Test
    void aFilteredEmptyListOffersToClearTheFilters()
    {
        AssessmentsView view = view(List.of(), "TEST", "", true);

        assertEquals("No assessment matches these filters.",
                view.getAssessmentsEmpty().getTitle());
        assertEquals("Clear filters", view.getAssessmentsEmpty().getActionLabel());
        assertEquals("/app/assessments", view.getAssessmentsEmpty().getActionHref());
    }

    @Test
    void aFilteredEmptyListByCourseOffersToClearTheFilters()
    {
        AssessmentsView view = view(List.of(), "", "course-1", true);

        assertEquals("No assessment matches these filters.",
                view.getAssessmentsEmpty().getTitle());
        assertEquals("Clear filters", view.getAssessmentsEmpty().getActionLabel());
    }

    @Test
    void filterOptionsKeepTheirSelection()
    {
        AssessmentsView view = view(List.of(),
                "EXAM", "course-1", true);

        assertEquals("selected", view.getTypeOptions().stream()
                .filter(option -> option.getValue().equals("EXAM")).findFirst().orElseThrow()
                .getSelectedAttribute());
        assertEquals("selected", view.getCourseOptions().stream()
                .filter(option -> option.getValue().equals("course-1")).findFirst().orElseThrow()
                .getSelectedAttribute());
    }

    @Test
    void rowsKeepTheirTypeStatusAndEligibilityLink()
    {
        AssessmentRow row = row("Final exam");

        assertEquals("Exam", row.getType());
        assertEquals("Active", row.getStatus().getLabel());
        assertEquals("/app/eligibility?assessment=assessment-1", row.getEligibilityHref());
        assertEquals("20", row.getMaximumScore());
        assertEquals(90, row.getDurationMinutes());
    }

    private static AssessmentsView view(List<AssessmentRow> rows, String typeFilter,
                                        String courseFilter, boolean canWrite)
    {
        return new AssessmentsView(rows,
                List.of(Option.of("", "All types", typeFilter.isBlank()),
                        Option.of("EXAM", "Exam", "EXAM".equals(typeFilter)),
                        Option.of("TEST", "Test", "TEST".equals(typeFilter))),
                List.of(Option.of("", "All courses", courseFilter.isBlank()),
                        Option.of("course-1", "MATH101 - Mathematics",
                                "course-1".equals(courseFilter))),
                List.of(Option.of("course-1", "MATH101 - Mathematics")),
                List.of(Option.of("EXAM", "Exam"), Option.of("TEST", "Test")),
                typeFilter, courseFilter, canWrite, Map.of(), Map.of());
    }

    private static AssessmentRow row(String title)
    {
        return new AssessmentRow("assessment-1", title, "Exam",
                "MATH101 - Mathematics", "20", 90, Badge.active(true));
    }
}
