package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Management page for exam sessions. */
public final class ExamSessionsView implements PageView
{
    private final PageChrome chrome;
    private final List<ManagedSessionRow> sessions;
    private final List<Option> selectableSessions;
    private final List<Option> assessments;
    private final List<Option> rooms;
    private final String query;
    private final Map<String, String> sessionForm;
    private final Map<String, String> sessionErrors;
    private final Map<String, String> invigilatorForm;
    private final Map<String, String> invigilatorErrors;
    private final Map<String, String> cancelErrors;
    private final String countLabel;
    private final EmptyState sessionsEmpty;
    private final EmptyState selectableSessionsEmpty;

    /**
     * @param sessions visible session rows
     * @param selectableSessions every session, as select options
     * @param assessments known assessment definitions, as datalist options
     * @param rooms rooms, as select options
     * @param query current search text
     * @param canWrite whether the account may schedule, update or cancel
     * @param sessionForm submitted schedule values
     * @param sessionErrors schedule field errors
     * @param invigilatorForm submitted invigilator values
     * @param invigilatorErrors invigilator field errors
     * @param cancelErrors cancellation errors
     */
    public ExamSessionsView(List<ManagedSessionRow> sessions,
                            List<Option> selectableSessions,
                            List<Option> assessments,
                            List<Option> rooms,
                            String query,
                            boolean canWrite,
                            Map<String, String> sessionForm,
                            Map<String, String> sessionErrors,
                            Map<String, String> invigilatorForm,
                            Map<String, String> invigilatorErrors,
                            Map<String, String> cancelErrors)
    {
        this.chrome = PageChrome.builder("Manage exam sessions", "Examinations",
                        "examinations", "/WEB-INF/views/exam-sessions/index.jsp")
                .description("Schedule sessions, find one by its id, change its "
                        + "invigilators or cancel it. A session is stored only after the "
                        + "booking service confirmed the room and the invigilators for that "
                        + "time range. Times are entered and shown in UTC.")
                .breadcrumb("Overview", "/app")
                .section("Examinations", "/app/examinations")
                .headerAction("View examinations", "/app/examinations")
                .writable(canWrite)
                .build();
        this.sessions = List.copyOf(sessions);
        this.selectableSessions = List.copyOf(selectableSessions);
        this.assessments = List.copyOf(assessments);
        this.rooms = List.copyOf(rooms);
        this.query = Objects.requireNonNull(query, "query is required");
        this.sessionForm = Map.copyOf(sessionForm);
        this.sessionErrors = Map.copyOf(sessionErrors);
        this.invigilatorForm = Map.copyOf(invigilatorForm);
        this.invigilatorErrors = Map.copyOf(invigilatorErrors);
        this.cancelErrors = Map.copyOf(cancelErrors);
        this.countLabel = sessions.size() + " of "
                + Views.count(selectableSessions.size(), "session", "sessions") + " shown.";

        if (!query.isBlank())
        {
            this.sessionsEmpty = EmptyState.withAction("No session matches this id.",
                    "Try a shorter id or clear the search.",
                    "Clear search", "/app/exam-sessions");
        }
        else
        {
            this.sessionsEmpty = EmptyState.of("No exam session yet.",
                    "Schedule the first session below.");
        }

        this.selectableSessionsEmpty = EmptyState.of("No session to update.",
                "Schedule a session first.");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<ManagedSessionRow> getSessions()
    {
        return sessions;
    }

    public List<Option> getSelectableSessions()
    {
        return selectableSessions;
    }

    public List<Option> getAssessments()
    {
        return assessments;
    }

    public List<Option> getRooms()
    {
        return rooms;
    }

    public String getQuery()
    {
        return query;
    }

    public Map<String, String> getSessionForm()
    {
        return sessionForm;
    }

    public Map<String, String> getSessionErrors()
    {
        return sessionErrors;
    }

    public Map<String, String> getInvigilatorForm()
    {
        return invigilatorForm;
    }

    public Map<String, String> getInvigilatorErrors()
    {
        return invigilatorErrors;
    }

    public Map<String, String> getCancelErrors()
    {
        return cancelErrors;
    }

    public String getCountLabel()
    {
        return countLabel;
    }

    public EmptyState getSessionsEmpty()
    {
        return sessionsEmpty;
    }

    public EmptyState getSelectableSessionsEmpty()
    {
        return selectableSessionsEmpty;
    }
}
