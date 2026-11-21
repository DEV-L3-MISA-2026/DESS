package com.dess.platform.web.view;

import java.util.Objects;

/** One read-only exam-session row on the examinations page. */
public final class SessionRow
{
    private final String startsAt;
    private final String endsAt;
    private final String room;
    private final int candidateCount;
    private final int invigilatorCount;

    /**
     * @param startsAt formatted UTC start
     * @param endsAt formatted UTC end
     * @param room room label
     * @param candidateCount linked candidates
     * @param invigilatorCount assigned invigilators
     */
    public SessionRow(String startsAt, String endsAt, String room, int candidateCount,
                      int invigilatorCount)
    {
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt is required");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt is required");
        this.room = Objects.requireNonNull(room, "room is required");
        this.candidateCount = candidateCount;
        this.invigilatorCount = invigilatorCount;
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

    public int getInvigilatorCount()
    {
        return invigilatorCount;
    }
}
