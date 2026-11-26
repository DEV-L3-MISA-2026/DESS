package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsViewTest
{
    @Test
    void listWithoutASelectedSessionHasNoEntrySection()
    {
        ResultsView view = new ResultsView(List.of(sessionRow()));

        assertFalse(view.isHasEntry());
        assertNull(view.getEntry());
        assertEquals("1 session. Times are in UTC. Results are counted per assessment.",
                view.getCountLabel());
    }

    @Test
    void entrySectionCountsEnteredCandidates()
    {
        ResultsView view = new ResultsView(List.of(sessionRow()), entry(), true);

        assertTrue(view.isHasEntry());
        assertEquals("1 of 2 candidates entered. The page shows only the stored mark.",
                view.getEntry().getCountLabel());
        assertTrue(view.getChrome().isWritable());
    }

    @Test
    void entrySectionWithoutWritePermissionIsReadOnly()
    {
        ResultsView view = new ResultsView(List.of(sessionRow()), entry(), false);

        assertTrue(view.isHasEntry());
        assertFalse(view.getChrome().isWritable());
    }

    @Test
    void entryRowsShowTheStoredMarkOnly()
    {
        ResultEntry entry = entry();

        assertEquals("16", entry.getRows().get(0).getStoredScore());
        assertEquals("Scored", entry.getRows().get(0).getStatus().getLabel());
        assertEquals("—", entry.getRows().get(1).getStoredScore());
        assertEquals("Not entered", entry.getRows().get(1).getStatus().getLabel());
    }

    @Test
    void entryContextIdentifiesTheAssessmentCourseAndSession()
    {
        ResultEntry entry = entry();

        assertEquals("Final exam", entry.getAssessmentTitle());
        assertEquals("MATH101 - Mathematics", entry.getCourse());
        assertEquals("Exam", entry.getType());
        assertEquals("20", entry.getMaximumScore());
        assertEquals("2026-11-28 09:00", entry.getSessionStartsAt());
        assertEquals("B202 - Lab", entry.getRoom());
    }

    @Test
    void anEntryWithoutCandidatesNamesTheNextAction()
    {
        ResultEntry entry = new ResultEntry("session-1", "assessment-1", "Final exam",
                "MATH101 - Mathematics", "Exam", "20", "2026-11-28 09:00", "2026-11-28 11:00",
                "B202 - Lab", List.of(), Map.of(), Map.of());

        assertEquals("No candidate yet.", entry.getRowsEmpty().getTitle());
    }

    private static ResultRow sessionRow()
    {
        return new ResultRow("session-1", "2026-11-28 09:00", "2026-11-28 11:00",
                "B202 - Lab", 2, Badge.of("warn", "1 of 2 entered"));
    }

    private static ResultEntry entry()
    {
        return new ResultEntry("session-1", "assessment-1", "Final exam",
                "MATH101 - Mathematics", "Exam", "20", "2026-11-28 09:00", "2026-11-28 11:00",
                "B202 - Lab",
                List.of(
                        new ResultEntryRow(1L, "Ada Lovelace", "STU-001", "16",
                                Badge.resultStatus(
                                        com.dess.assessment.domain.ResultStatus.SCORED),
                                "result-1",
                                List.of(new CorrectionRow("14", "Scored", "16", "Scored",
                                        "Fabien Halaby", "2026-11-26 10:00"))),
                        new ResultEntryRow(2L, "Grace Hopper", "STU-002", "—",
                                Badge.of("off", "Not entered"), "", List.of())),
                Map.of(), Map.of());
    }
}
