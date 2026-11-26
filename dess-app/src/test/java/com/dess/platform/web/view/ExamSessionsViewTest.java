package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamSessionsViewTest
{
    @Test
    void countLabelShowsVisibleAndTotalSessions()
    {
        ExamSessionsView view = view("", List.of());

        assertEquals("0 of 2 sessions shown.", view.getCountLabel());
    }

    @Test
    void anEmptyFirstVisitExplainsHowToSchedule()
    {
        ExamSessionsView view = view("", List.of());

        assertEquals("No exam session yet.", view.getSessionsEmpty().getTitle());
        assertEquals("", view.getSessionsEmpty().getActionLabel());
    }

    @Test
    void aSearchWithNoMatchOffersToClearIt()
    {
        ExamSessionsView view = view("abc", List.of());

        assertEquals("No session matches this id.", view.getSessionsEmpty().getTitle());
        assertEquals("Clear search", view.getSessionsEmpty().getActionLabel());
        assertEquals("/app/exam-sessions", view.getSessionsEmpty().getActionHref());
    }

    @Test
    void optionsKeepTheirSelection()
    {
        ExamSessionsView view = new ExamSessionsView(List.of(),
                List.of(Option.of("s-1", "2026-11-21 10:00 - B202", true)),
                List.of(), List.of(Option.of("room-1", "B202 - Lab", true)),
                "", true, Map.of("roomId", "room-1"), Map.of(),
                Map.of("sessionId", "s-1"), Map.of(), Map.of());

        assertEquals("selected", view.getSelectableSessions().get(0).getSelectedAttribute());
        assertEquals("selected", view.getRooms().get(0).getSelectedAttribute());
        assertTrue(view.getChrome().writable());
    }

    private static ExamSessionsView view(String query, List<ManagedSessionRow> sessions)
    {
        return new ExamSessionsView(sessions,
                List.of(Option.of("s-1", "2026-11-21 10:00 - B202"),
                        Option.of("s-2", "2026-11-22 10:00 - B203")),
                List.of(), List.of(), query, true,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }
}
