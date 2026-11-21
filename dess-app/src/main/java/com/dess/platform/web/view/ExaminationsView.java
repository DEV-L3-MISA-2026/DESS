package com.dess.platform.web.view;

import java.util.List;

/** Read-only exam-session list on the examinations page. */
public final class ExaminationsView implements PageView
{
    private final PageChrome chrome;
    private final List<SessionRow> sessions;
    private final String countLabel;
    private final EmptyState sessionsEmpty;

    /**
     * @param sessions exam sessions
     */
    public ExaminationsView(List<SessionRow> sessions)
    {
        this.chrome = PageChrome.builder("Examinations", "Exam sessions", "examinations",
                        "/WEB-INF/views/examinations/index.jsp")
                .description("Exam sessions: one assessment, in one room, at one time, "
                        + "with its candidates and invigilators. Sessions are created on "
                        + "the management page.")
                .breadcrumb("Overview", "/app")
                .headerAction("Manage sessions", "/app/exam-sessions")
                .build();
        this.sessions = List.copyOf(sessions);
        this.countLabel = Views.count(sessions.size(), "session", "sessions")
                + ". Times are in UTC.";
        this.sessionsEmpty = EmptyState.withAction("No exam session yet.",
                "Exam sessions appear here once they are scheduled. Candidates are "
                        + "linked once eligibility is known, and invigilators are assigned "
                        + "afterwards.",
                "Manage sessions", "/app/exam-sessions");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<SessionRow> getSessions()
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
