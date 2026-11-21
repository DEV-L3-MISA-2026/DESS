package com.dess.platform.web.view;

import java.util.List;

/** Results by assessment on the results page. */
public final class ResultsView implements PageView
{
    private final PageChrome chrome;
    private final List<ResultRow> sessions;
    private final String countLabel;
    private final EmptyState sessionsEmpty;

    /**
     * @param sessions exam sessions with result progress
     */
    public ResultsView(List<ResultRow> sessions)
    {
        this.chrome = PageChrome.builder("Results", "Results by assessment", "results",
                        "/WEB-INF/views/results/index.jsp")
                .description("Each exam session with the number of results recorded for its "
                        + "assessment. The page never shows a mark that was not stored, and "
                        + "result entry is not available here yet.")
                .breadcrumb("Overview", "/app")
                .headerAction("Open examinations", "/app/examinations")
                .build();
        this.sessions = List.copyOf(sessions);
        this.countLabel = Views.count(sessions.size(), "session", "sessions")
                + ". Times are in UTC. Results are counted per assessment.";
        this.sessionsEmpty = EmptyState.withAction("No exam session yet.",
                "Results are counted per assessment once an exam session exists. "
                        + "Create one from the examinations page first.",
                "Open examinations", "/app/examinations");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<ResultRow> getSessions()
    {
        return sessions;
    }

    public String getCountLabel()
    {
        return countLabel;
    }

    public EmptyState getSessionsEmpty()
    {
        return sessionsEmpty;
    }
}
