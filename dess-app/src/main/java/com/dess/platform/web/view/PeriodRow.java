package com.dess.platform.web.view;

import java.util.Objects;

/** One academic period row. */
public final class PeriodRow
{
    private final String academicYear;
    private final int term;

    /**
     * @param academicYear academic year label
     * @param term term number
     */
    public PeriodRow(String academicYear, int term)
    {
        this.academicYear = Objects.requireNonNull(academicYear,
                "academicYear is required");
        this.term = term;
    }

    public String getAcademicYear()
    {
        return academicYear;
    }

    public int getTerm()
    {
        return term;
    }

    public String getTermLabel()
    {
        return "Term " + term;
    }
}
