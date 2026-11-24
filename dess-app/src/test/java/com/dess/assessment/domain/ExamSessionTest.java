package com.dess.assessment.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ExamSessionTest
{
    @Test
    void createsSessionWithoutCandidatesOrInvigilators()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-23T09:00:00+03:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-11-23T11:00:00+03:00");
        UUID assessmentId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ExamSession session = ExamSession.create(assessmentId, roomId, start, end);

        assertEquals(assessmentId, session.assessmentId());
        assertEquals(roomId, session.roomId());
        assertEquals(ZoneOffset.UTC, session.startsAt().getOffset());
        assertEquals(start.withOffsetSameInstant(ZoneOffset.UTC), session.startsAt());
        assertEquals(List.of(), session.candidatePersonIds());
        assertEquals(List.of(), session.invigilatorPersonIds());
    }

    @Test
    void linksCandidatesAndInvigilators()
    {
        ExamSession session = session().withCandidates(List.of(11L, 12L)).withInvigilators(List.of(21L));

        assertEquals(List.of(11L, 12L), session.candidatePersonIds());
        assertEquals(List.of(21L), session.invigilatorPersonIds());
    }

    @Test
    void keepsItsOwnCopyOfPersonLists()
    {
        ExamSession session = session().withCandidates(List.of(11L, 12L));

        assertThrows(UnsupportedOperationException.class,
                () -> session.candidatePersonIds().add(13L));
    }

    @Test
    void detectsOverlappingSessions()
    {
        ExamSession first = session();
        ExamSession overlapping = ExamSession.create(UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-23T10:00:00Z"),
                OffsetDateTime.parse("2026-11-23T12:00:00Z"));
        ExamSession adjacent = ExamSession.create(UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-23T11:00:00Z"),
                OffsetDateTime.parse("2026-11-23T12:00:00Z"));

        assertTrue(first.overlaps(overlapping));
        assertFalse(first.overlaps(adjacent));
    }

    @Test
    void rejectsZeroLengthRange()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-23T09:00:00Z");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ExamSession.create(UUID.randomUUID(), UUID.randomUUID(), start, start));

        assertEquals("startsAt must be before endsAt", exception.getMessage());
    }

    @Test
    void rejectsReversedRange()
    {
        assertThrows(IllegalArgumentException.class,
                () -> ExamSession.create(UUID.randomUUID(), UUID.randomUUID(),
                        OffsetDateTime.parse("2026-11-23T11:00:00Z"),
                        OffsetDateTime.parse("2026-11-23T09:00:00Z")));
    }

    @Test
    void rejectsMissingAssessmentOrRoom()
    {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-23T09:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-11-23T11:00:00Z");

        assertThrows(NullPointerException.class,
                () -> ExamSession.create(null, UUID.randomUUID(), start, end));
        assertThrows(NullPointerException.class,
                () -> ExamSession.create(UUID.randomUUID(), null, start, end));
    }

    @Test
    void rejectsDuplicateCandidate()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> session().withCandidates(List.of(11L, 11L)));

        assertEquals("a candidate must appear only once", exception.getMessage());
    }

    @Test
    void rejectsDuplicateInvigilator()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> session().withInvigilators(List.of(21L, 21L)));

        assertEquals("an invigilator must appear only once", exception.getMessage());
    }

    @Test
    void rejectsPersonAsBothCandidateAndInvigilator()
    {
        ExamSession session = session().withCandidates(List.of(11L, 12L));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> session.withInvigilators(List.of(12L)));

        assertEquals("a person cannot be both a candidate and an invigilator",
                exception.getMessage());
    }

    @Test
    void rejectsNullPersonId()
    {
        List<Long> candidates = new java.util.ArrayList<>();
        candidates.add(null);

        assertThrows(IllegalArgumentException.class, () -> session().withCandidates(candidates));
    }

    private static ExamSession session()
    {
        return ExamSession.create(UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-11-23T09:00:00Z"),
                OffsetDateTime.parse("2026-11-23T11:00:00Z"));
    }
}
