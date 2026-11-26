package com.dess.platform.web.view;

import java.util.Objects;

/** One entry of the correction trail of a result, as shown on the results page. */
public final class CorrectionRow
{
    private final String previousScore;
    private final String previousStatus;
    private final String newScore;
    private final String newStatus;
    private final String correctedBy;
    private final String correctedAt;

    /**
     * @param previousScore previous stored score, or {@code —} for an absent or
     *                      excused value
     * @param previousStatus previous stored status label
     * @param newScore new stored score, or {@code —} for an absent or excused value
     * @param newStatus new stored status label
     * @param correctedBy who corrected the result
     * @param correctedAt when the correction was made, in UTC
     */
    public CorrectionRow(String previousScore, String previousStatus, String newScore,
                         String newStatus, String correctedBy, String correctedAt)
    {
        this.previousScore = Objects.requireNonNull(previousScore, "previousScore is required");
        this.previousStatus = Objects.requireNonNull(previousStatus, "previousStatus is required");
        this.newScore = Objects.requireNonNull(newScore, "newScore is required");
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus is required");
        this.correctedBy = Objects.requireNonNull(correctedBy, "correctedBy is required");
        this.correctedAt = Objects.requireNonNull(correctedAt, "correctedAt is required");
    }

    public String getPreviousScore()
    {
        return previousScore;
    }

    public String getPreviousStatus()
    {
        return previousStatus;
    }

    public String getNewScore()
    {
        return newScore;
    }

    public String getNewStatus()
    {
        return newStatus;
    }

    public String getCorrectedBy()
    {
        return correctedBy;
    }

    public String getCorrectedAt()
    {
        return correctedAt;
    }
}
