package com.dess.platform.web.view;

import java.util.List;

/** Results by assessment on the results page. */
public final class ResultsView implements PageView
{
    private final PageChrome chrome;
    private final List<ResultRow> sessions;
    private final String countLabel;
    private final EmptyState sessionsEmpty;
    private final ResultEntry entry;

    /**
     * @param sessions exam sessions with result progress
     */
    public ResultsView(List<ResultRow> sessions)
    {
        this(sessions, null, false);
    }

    /**
     * @param sessions exam sessions with result progress
     * @param entry result entry section for one session, or {@code null} when no
     *              session is selected
     * @param canWrite whether the account may enter or correct results
     */
    public ResultsView(List<ResultRow> sessions, ResultEntry entry, boolean canWrite)
    {
        this.chrome = PageChrome.builder("Results", "Results by assessment", "results",
                        "/WEB-INF/views/results/index.jsp")
                .description("Each exam session with the number of results recorded for its "
                        + "assessment. The page never shows a mark that was not stored. "
                        + "Open a session to enter or correct its results.")
                .breadcrumb("Overview", "/app")
                .headerAction("Open examinations", "/app/examinations")
                .writable(canWrite)
                .build();
        this.sessions = List.copyOf(sessions);
        this.countLabel = Views.count(sessions.size(), "session", "sessions")
                + ". Times are in UTC. Results are counted per assessment.";
        this.sessionsEmpty = EmptyState.withAction("No exam session yet.",
                "Results are counted per assessment once an exam session exists. "
                        + "Create one from the examinations page first.",
                "Open examinations", "/app/examinations");
        this.entry = entry;
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

    /**
     * @return the result entry section, or {@code null} when no session is selected
     */
    public ResultEntry getEntry()
    {
        return entry;
    }

    /**
     * @return whether a session is selected for result entry
     */
    public boolean isHasEntry()
    {
        return entry != null;
    }
}
