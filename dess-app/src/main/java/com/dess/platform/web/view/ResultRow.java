package com.dess.platform.web.view;

import java.util.Objects;

/** One exam-session row with its result progress on the results page. */
public final class ResultRow
{
    private final String startsAt;
    private final String endsAt;
    private final String room;
    private final int candidateCount;
    private final Badge result;

    /**
     * @param startsAt formatted UTC start
     * @param endsAt formatted UTC end
     * @param room room label
     * @param candidateCount linked candidates
     * @param result result-progress badge
     */
    public ResultRow(String startsAt, String endsAt, String room, int candidateCount,
                     Badge result)
    {
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt is required");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt is required");
        this.room = Objects.requireNonNull(room, "room is required");
        this.candidateCount = candidateCount;
        this.result = Objects.requireNonNull(result, "result is required");
    }

    public String getStartsAt()
    {
        return startsAt;
    }

    public String getEndsAt()
    {
        return endsAt;
    }

    public String getRoom()
    {
        return room;
    }

    public int getCandidateCount()
    {
        return candidateCount;
    }

    public Badge getResult()
    {
        return result;
    }
}
