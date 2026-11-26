package com.dess.platform.web.view;

import java.util.List;
import java.util.Objects;

/**
 * One candidate on the result entry section of the results page.
 *
 * <p>Only the stored value is shown: the score and status come from the
 * result as saved, never from an unsubmitted form value. A candidate with
 * no stored result shows no mark at all.</p>
 */
public final class ResultEntryRow
{
    private final long studentPersonId;
    private final String name;
    private final String registrationNumber;
    private final String storedScore;
    private final Badge status;
    private final String resultId;
    private final List<CorrectionRow> corrections;

    /**
     * @param studentPersonId numeric person id of the candidate
     * @param name candidate name, or {@code Unknown person} when no person exists
     * @param registrationNumber student registration number, or an empty string
     *                           when the person is not a known student
     * @param storedScore stored score, or {@code —} when the result carries no
     *                    score or no result exists yet
     * @param status badge of the stored status, or {@code Not entered} when no
     *               result exists yet
     * @param resultId id of the stored result, or an empty string when no result
     *                 exists yet
     * @param corrections correction trail of the stored result, oldest first;
     *                    empty when the result was never corrected
     */
    public ResultEntryRow(long studentPersonId, String name, String registrationNumber,
                          String storedScore, Badge status, String resultId,
                          List<CorrectionRow> corrections)
    {
        this.studentPersonId = studentPersonId;
        this.name = Objects.requireNonNull(name, "name is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber,
                "registrationNumber is required");
        this.storedScore = Objects.requireNonNull(storedScore, "storedScore is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.resultId = Objects.requireNonNull(resultId, "resultId is required");
        this.corrections = List.copyOf(corrections);
    }

    public long getStudentPersonId()
    {
        return studentPersonId;
    }

    public String getName()
    {
        return name;
    }

    public String getRegistrationNumber()
    {
        return registrationNumber;
    }

    public String getStoredScore()
    {
        return storedScore;
    }

    public Badge getStatus()
    {
        return status;
    }

    public String getResultId()
    {
        return resultId;
    }

    public List<CorrectionRow> getCorrections()
    {
        return corrections;
    }
}
