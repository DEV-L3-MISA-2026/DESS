package com.dess.platform.web.view;

import java.util.Objects;

/** One row on the exam-session management page. */
public final class ManagedSessionRow
{
    private final String id;
    private final String assessment;
    private final String startsAt;
    private final String endsAt;
    private final String room;
    private final int candidateCount;
    private final String invigilators;
    private final Badge booking;

    /**
     * @param id session id
     * @param assessment assessment title or unknown label
     * @param startsAt formatted UTC start
     * @param endsAt formatted UTC end
     * @param room room label
     * @param candidateCount linked candidates
     * @param invigilators comma-separated invigilator ids
     */
    public ManagedSessionRow(String id, String assessment, String startsAt, String endsAt,
                             String room, int candidateCount, String invigilators)
    {
        this.id = Objects.requireNonNull(id, "id is required");
        this.assessment = Objects.requireNonNull(assessment, "assessment is required");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt is required");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt is required");
        this.room = Objects.requireNonNull(room, "room is required");
        this.candidateCount = candidateCount;
        this.invigilators = Objects.requireNonNull(invigilators, "invigilators is required");
        this.booking = Badge.confirmed();
    }

    public String getId()
    {
        return id;
    }

    public String getAssessment()
    {
        return assessment;
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

    public String getInvigilators()
    {
        return invigilators;
    }

    public Badge getBooking()
    {
        return booking;
    }
}
