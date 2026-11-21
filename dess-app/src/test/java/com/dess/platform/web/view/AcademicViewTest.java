package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicViewTest
{
    @Test
    void countLabelsUseTheRightNoun()
    {
        AcademicView view = new AcademicView(
                List.of(new PeriodRow("2026-2027", 1)),
                List.of(new ProgramRow("BD", "Business", true)),
                List.of(new CourseRow("CS101", "Algorithms", true)),
                false, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertEquals("1 period.", view.getPeriodCountLabel());
        assertEquals("1 program.", view.getProgramCountLabel());
        assertEquals("1 course in the catalogue.", view.getCourseCountLabel());
    }

    @Test
    void theSubmittedTermIsSelected()
    {
        AcademicView view = new AcademicView(List.of(), List.of(), List.of(), true,
                Map.of("academicYear", "2026-2027", "term", "2"),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertEquals("2", selectedTerm(view));
    }

    @Test
    void noTermIsSelectedOnAFirstVisit()
    {
        AcademicView view = new AcademicView(List.of(), List.of(), List.of(), true,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertEquals("", selectedTerm(view));
    }

    @Test
    void emptySectionsExplainWhatToAdd()
    {
        AcademicView view = new AcademicView(List.of(), List.of(), List.of(), false,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        assertTrue(view.getPeriodsEmpty().getMessage().contains("year and term"));
        assertTrue(view.getCoursesEmpty().getMessage().contains("first course"));
    }

    private static String selectedTerm(AcademicView view)
    {
        for (Option option : view.getTermOptions())
        {
            if ("selected".equals(option.getSelectedAttribute()))
            {
                return option.getValue();
            }
        }

        return "";
    }
}
