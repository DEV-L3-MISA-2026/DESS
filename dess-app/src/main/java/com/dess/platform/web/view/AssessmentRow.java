package com.dess.platform.web.view;

import java.util.Objects;

/** One assessment definition on the assessments page. */
public final class AssessmentRow
{
    private final String id;
    private final String title;
    private final String type;
    private final String course;
    private final String maximumScore;
    private final int durationMinutes;
    private final Badge status;
    private final String eligibilityHref;

    /**
     * @param id assessment id
     * @param title assessment title
     * @param type type label, for example {@code Exam}
     * @param course course label, for example {@code MATH101 - Mathematics}
     * @param maximumScore maximum score as entered, without formatting
     * @param durationMinutes duration in minutes
     * @param status active or inactive badge
     */
    public AssessmentRow(String id, String title, String type, String course,
                         String maximumScore, int durationMinutes, Badge status)
    {
        this.id = Objects.requireNonNull(id, "id is required");
        this.title = Objects.requireNonNull(title, "title is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.course = Objects.requireNonNull(course, "course is required");
        this.maximumScore = Objects.requireNonNull(maximumScore, "maximumScore is required");
        this.durationMinutes = durationMinutes;
        this.status = Objects.requireNonNull(status, "status is required");
        this.eligibilityHref = "/app/eligibility?assessment=" + id;
    }

    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getType()
    {
        return type;
    }

    public String getCourse()
    {
        return course;
    }

    public String getMaximumScore()
    {
        return maximumScore;
    }

    public int getDurationMinutes()
    {
        return durationMinutes;
    }

    public Badge getStatus()
    {
        return status;
    }

    public String getEligibilityHref()
    {
        return eligibilityHref;
    }
}
