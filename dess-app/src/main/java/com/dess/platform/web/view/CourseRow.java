package com.dess.platform.web.view;

import java.util.Objects;

/** One course row on the academic page. */
public final class CourseRow
{
    private final String code;
    private final String title;
    private final Badge status;

    /**
     * @param code course code
     * @param title course title
     * @param active whether the course is active
     */
    public CourseRow(String code, String title, boolean active)
    {
        this.code = Objects.requireNonNull(code, "code is required");
        this.title = Objects.requireNonNull(title, "title is required");
        this.status = Badge.active(active);
    }

    public String getCode()
    {
        return code;
    }

    public String getTitle()
    {
        return title;
    }

    public Badge getStatus()
    {
        return status;
    }
}
