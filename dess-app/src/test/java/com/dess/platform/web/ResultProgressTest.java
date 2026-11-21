package com.dess.platform.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultProgressTest
{
    @Test
    void explainsWhenNoCandidateIsLinked()
    {
        ResultProgress progress = ResultProgress.of(0, 0);

        assertEquals("No candidate yet", progress.label());
        assertEquals("info", progress.tone());
    }

    @Test
    void explainsWhenCandidatesHaveNoResultYet()
    {
        ResultProgress progress = ResultProgress.of(4, 0);

        assertEquals("Not entered", progress.label());
        assertEquals("off", progress.tone());
    }

    @Test
    void countsPartialAndCompleteEntry()
    {
        ResultProgress partial = ResultProgress.of(5, 2);
        ResultProgress complete = ResultProgress.of(5, 5);

        assertEquals("2 of 5 entered", partial.label());
        assertEquals("warn", partial.tone());
        assertEquals("5 of 5 entered", complete.label());
        assertEquals("ok", complete.tone());
    }

    @Test
    void reportsMoreRecordedResultsThanLinkedCandidates()
    {
        ResultProgress progress = ResultProgress.of(2, 3);

        assertEquals("3 recorded", progress.label());
        assertEquals("warn", progress.tone());
    }

    @Test
    void rejectsNegativeCounts()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultProgress.of(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> ResultProgress.of(0, -1));
    }
}
