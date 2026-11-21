package com.dess.assessment.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A concrete session of one assessment: one exam, in one room, at one
 * time, with its candidates and its invigilators.
 *
 * <p>The assessment module owns this type. Scheduling only knows the
 * booking that reserves the room, the group and the invigilators for
 * the same time range. The assessmentId refers to the Assessment
 * definition owned by the assessment definitions work. It is a UUID
 * here because that definition is not implemented yet, so there is no
 * type to link to.</p>
 *
 * <p>Rules of the model:</p>
 * <ul>
 *   <li>The time range is half open, [startsAt, endsAt). A session
 *       that starts exactly when another one ends does not overlap
 *       it.</li>
 *   <li>Instants are stored in UTC. Any offset given by a caller is
 *       converted to UTC on construction.</li>
 *   <li>Candidates and invigilators are Person ids, the numeric ids
 *       of the people module. A list never contains a null, never
 *       contains the same person twice, and no person is both a
 *       candidate and an invigilator of the same session.</li>
 *   <li>A session is created without candidates and without
 *       invigilators. Candidates are linked once eligibility is
 *       known, and invigilators are assigned afterwards. Use
 *       {@link #withCandidates(List)} and
 *       {@link #withInvigilators(List)} for that.</li>
 * </ul>
 */
public record ExamSession(UUID id, UUID assessmentId, UUID roomId,
                          OffsetDateTime startsAt, OffsetDateTime endsAt,
                          List<Long> candidatePersonIds, List<Long> invigilatorPersonIds,
                          OffsetDateTime createdAt)
{
    public ExamSession
    {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(assessmentId, "assessmentId is required");
        Objects.requireNonNull(roomId, "roomId is required");
        Objects.requireNonNull(startsAt, "startsAt is required");
        Objects.requireNonNull(endsAt, "endsAt is required");
        if (!startsAt.isBefore(endsAt))
        {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
        Objects.requireNonNull(candidatePersonIds, "candidatePersonIds are required");
        Objects.requireNonNull(invigilatorPersonIds, "invigilatorPersonIds are required");
        if (candidatePersonIds.stream().anyMatch(Objects::isNull)
                || invigilatorPersonIds.stream().anyMatch(Objects::isNull))
        {
            throw new IllegalArgumentException("person id is required");
        }
        if (new HashSet<>(candidatePersonIds).size() != candidatePersonIds.size())
        {
            throw new IllegalArgumentException("a candidate must appear only once");
        }
        if (new HashSet<>(invigilatorPersonIds).size() != invigilatorPersonIds.size())
        {
            throw new IllegalArgumentException("an invigilator must appear only once");
        }
        if (candidatePersonIds.stream().anyMatch(invigilatorPersonIds::contains))
        {
            throw new IllegalArgumentException(
                    "a person cannot be both a candidate and an invigilator");
        }
        Objects.requireNonNull(createdAt, "createdAt is required");
        startsAt = startsAt.withOffsetSameInstant(ZoneOffset.UTC);
        endsAt = endsAt.withOffsetSameInstant(ZoneOffset.UTC);
        createdAt = createdAt.withOffsetSameInstant(ZoneOffset.UTC);
        candidatePersonIds = List.copyOf(candidatePersonIds);
        invigilatorPersonIds = List.copyOf(invigilatorPersonIds);
    }

    /**
     * Creates a session without candidates and without invigilators.
     * Candidates and invigilators are added later, once they are known.
     */
    public static ExamSession create(UUID assessmentId, UUID roomId,
                                     OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        return new ExamSession(UUID.randomUUID(), assessmentId, roomId, startsAt, endsAt,
                List.of(), List.of(), OffsetDateTime.now(ZoneOffset.UTC));
    }

    /** Returns a copy of this session with the given candidates. */
    public ExamSession withCandidates(List<Long> candidates)
    {
        return new ExamSession(id, assessmentId, roomId, startsAt, endsAt,
                candidates, invigilatorPersonIds, createdAt);
    }

    /** Returns a copy of this session with the given invigilators. */
    public ExamSession withInvigilators(List<Long> invigilators)
    {
        return new ExamSession(id, assessmentId, roomId, startsAt, endsAt,
                candidatePersonIds, invigilators, createdAt);
    }

    /**
     * Returns true when the time ranges of the two sessions overlap.
     * Ranges are half open, so a session that starts exactly when this
     * one ends does not overlap it.
     */
    public boolean overlaps(ExamSession other)
    {
        Objects.requireNonNull(other, "other is required");
        return startsAt.isBefore(other.endsAt) && endsAt.isAfter(other.startsAt);
    }
}
