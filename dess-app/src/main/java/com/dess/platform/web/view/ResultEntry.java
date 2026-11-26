package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The result entry section of the results page, for one exam session.
 *
 * <p>It identifies the assessment, the course and the session, lists the
 * session candidates with their stored result only, and carries the entry
 * form state so a validation error keeps the entered value. There is no
 * academic period to show yet: no course offering exists, so no period is
 * invented here.</p>
 */
public final class ResultEntry
{
    private final String sessionId;
    private final String assessmentId;
    private final String assessmentTitle;
    private final String course;
    private final String type;
    private final String maximumScore;
    private final String sessionStartsAt;
    private final String sessionEndsAt;
    private final String room;
    private final List<ResultEntryRow> rows;
    private final Map<String, String> form;
    private final Map<String, String> errors;
    private final String countLabel;
    private final EmptyState rowsEmpty;

    /**
     * @param sessionId exam session id
     * @param assessmentId assessment id, for the entry form
     * @param assessmentTitle title of the assessment
     * @param course course label, for example {@code MATH101 - Mathematics}
     * @param type type label, for example {@code Exam}
     * @param maximumScore maximum score as stored, without formatting
     * @param sessionStartsAt session start, formatted in UTC
     * @param sessionEndsAt session end, formatted in UTC
     * @param room room label
     * @param rows one row per session candidate, with the stored result only
     * @param form submitted entry values, kept after a validation error
     * @param errors entry field errors, keyed by the field DOM id
     */
    public ResultEntry(String sessionId, String assessmentId, String assessmentTitle,
                       String course, String type,
                       String maximumScore, String sessionStartsAt, String sessionEndsAt,
                       String room, List<ResultEntryRow> rows, Map<String, String> form,
                       Map<String, String> errors)
    {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId is required");
        this.assessmentId = Objects.requireNonNull(assessmentId, "assessmentId is required");
        this.assessmentTitle = Objects.requireNonNull(assessmentTitle, "assessmentTitle is required");
        this.course = Objects.requireNonNull(course, "course is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.maximumScore = Objects.requireNonNull(maximumScore, "maximumScore is required");
        this.sessionStartsAt = Objects.requireNonNull(sessionStartsAt, "sessionStartsAt is required");
        this.sessionEndsAt = Objects.requireNonNull(sessionEndsAt, "sessionEndsAt is required");
        this.room = Objects.requireNonNull(room, "room is required");
        this.rows = List.copyOf(rows);
        this.form = Map.copyOf(form);
        this.errors = Map.copyOf(errors);
        long entered = rows.stream().filter(row -> !row.getResultId().isEmpty()).count();
        this.countLabel = entered + " of " + Views.count(rows.size(), "candidate", "candidates")
                + " entered. The page shows only the stored mark.";
        this.rowsEmpty = EmptyState.of("No candidate yet.",
                "Link candidates to this exam session first.");
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public String getAssessmentId()
    {
        return assessmentId;
    }

    public String getAssessmentTitle()
    {
        return assessmentTitle;
    }

    public String getCourse()
    {
        return course;
    }

    public String getType()
    {
        return type;
    }

    public String getMaximumScore()
    {
        return maximumScore;
    }

    public String getSessionStartsAt()
    {
        return sessionStartsAt;
    }

    public String getSessionEndsAt()
    {
        return sessionEndsAt;
    }

    public String getRoom()
    {
        return room;
    }

    public List<ResultEntryRow> getRows()
    {
        return rows;
    }

    public Map<String, String> getForm()
    {
        return form;
    }

    public Map<String, String> getErrors()
    {
        return errors;
    }

    public String getCountLabel()
    {
        return countLabel;
    }

    public EmptyState getRowsEmpty()
    {
        return rowsEmpty;
    }
}
