package com.dess.assessment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ResultTest
{
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();

    @Test
    void createsAScoredResult()
    {
        Result result = Result.create(ASSESSMENT_ID, UUID.randomUUID(), 42L,
                new BigDecimal("15.50"), ResultStatus.SCORED, "professor@example.com");

        assertNotNull(result.id());
        assertEquals(ASSESSMENT_ID, result.assessmentId());
        assertEquals(42L, result.studentPersonId());
        assertEquals(new BigDecimal("15.50"), result.score());
        assertEquals(ResultStatus.SCORED, result.status());
        assertEquals("professor@example.com", result.enteredBy());
        assertNotNull(result.enteredAt());
        assertEquals(result.enteredAt(), result.updatedAt());
    }

    @Test
    void createsAnAbsentResultWithoutAScore()
    {
        Result result = Result.create(ASSESSMENT_ID, null, 42L,
                null, ResultStatus.ABSENT, "professor@example.com");

        assertNull(result.score());
        assertNull(result.examSessionId());
        assertEquals(ResultStatus.ABSENT, result.status());
    }

    @Test
    void convertsEntryTimeToUtc()
    {
        Result result = new Result(UUID.randomUUID(), ASSESSMENT_ID, null, 42L,
                new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com",
                OffsetDateTime.parse("2026-11-21T12:00:00+03:00"),
                OffsetDateTime.parse("2026-11-21T12:00:00+03:00"));

        assertEquals(ZoneOffset.UTC, result.enteredAt().getOffset());
        assertEquals(9, result.enteredAt().getHour());
    }

    @Test
    void correctionKeepsTheEntryAuditFields()
    {
        Result result = Result.create(ASSESSMENT_ID, null, 42L,
                new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com");

        Result corrected = result.corrected(new BigDecimal("12"), ResultStatus.SCORED);

        assertEquals(result.id(), corrected.id());
        assertEquals(new BigDecimal("12"), corrected.score());
        assertEquals("professor@example.com", corrected.enteredBy());
        assertEquals(result.enteredAt(), corrected.enteredAt());
        assertTrue(!corrected.updatedAt().isBefore(result.enteredAt()));
    }

    @Test
    void correctionCanMarkAStudentAbsent()
    {
        Result result = Result.create(ASSESSMENT_ID, null, 42L,
                new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com");

        Result corrected = result.corrected(null, ResultStatus.ABSENT);

        assertNull(corrected.score());
        assertEquals(ResultStatus.ABSENT, corrected.status());
    }

    @Test
    void rejectsAScoredResultWithoutAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                42L, null, ResultStatus.SCORED, "professor@example.com"));
    }

    @Test
    void rejectsAnAbsentResultWithAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                42L, new BigDecimal("10"), ResultStatus.ABSENT, "professor@example.com"));
    }

    @Test
    void rejectsAnExcusedResultWithAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                42L, new BigDecimal("10"), ResultStatus.EXCUSED, "professor@example.com"));
    }

    @Test
    void rejectsANegativeScore()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                42L, new BigDecimal("-1"), ResultStatus.SCORED, "professor@example.com"));
    }

    @Test
    void rejectsAScoreWithMoreThanTwoDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                42L, new BigDecimal("10.001"), ResultStatus.SCORED, "professor@example.com"));
    }

    @Test
    void rejectsAnInvalidStudentId()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                0L, new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com"));
    }

    @Test
    void rejectsABlankEnteredBy()
    {
        assertThrows(IllegalArgumentException.class, () -> Result.create(ASSESSMENT_ID, null,
                42L, new BigDecimal("10"), ResultStatus.SCORED, "  "));
    }
}
