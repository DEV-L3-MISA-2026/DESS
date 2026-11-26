package com.dess.assessment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ResultCorrectionTest
{
    private static final UUID RESULT_ID = UUID.randomUUID();

    @Test
    void createsAScoredToScoredCorrection()
    {
        ResultCorrection correction = ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, new BigDecimal("12"),
                ResultStatus.SCORED, "corrector@example.com",
                OffsetDateTime.now(ZoneOffset.UTC));

        assertNotNull(correction.id());
        assertEquals(RESULT_ID, correction.resultId());
        assertEquals(new BigDecimal("10"), correction.previousScore());
        assertEquals(new BigDecimal("12"), correction.newScore());
        assertEquals("corrector@example.com", correction.correctedBy());
        assertNotNull(correction.correctedAt());
    }

    @Test
    void createsAScoredToAbsentCorrectionWithoutANewScore()
    {
        ResultCorrection correction = ResultCorrection.create(RESULT_ID,
                new BigDecimal("12"), ResultStatus.SCORED, null,
                ResultStatus.ABSENT, "corrector@example.com",
                OffsetDateTime.now(ZoneOffset.UTC));

        assertEquals(new BigDecimal("12"), correction.previousScore());
        assertNull(correction.newScore());
        assertEquals(ResultStatus.ABSENT, correction.newStatus());
    }

    @Test
    void convertsCorrectionTimeToUtc()
    {
        ResultCorrection correction = ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, new BigDecimal("12"),
                ResultStatus.SCORED, "corrector@example.com",
                OffsetDateTime.parse("2026-11-21T12:00:00+03:00"));

        assertEquals(ZoneOffset.UTC, correction.correctedAt().getOffset());
        assertEquals(9, correction.correctedAt().getHour());
    }

    @Test
    void rejectsAScoredPreviousValueWithoutAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                null, ResultStatus.SCORED, new BigDecimal("12"), ResultStatus.SCORED,
                "corrector@example.com", OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void rejectsAnAbsentPreviousValueWithAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.ABSENT, new BigDecimal("12"),
                ResultStatus.SCORED, "corrector@example.com",
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void rejectsAScoredNewValueWithoutAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, null, ResultStatus.SCORED,
                "corrector@example.com", OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void rejectsAnExcusedNewValueWithAScore()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, new BigDecimal("12"),
                ResultStatus.EXCUSED, "corrector@example.com",
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void rejectsABlankCorrector()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, new BigDecimal("12"),
                ResultStatus.SCORED, "  ", OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void rejectsANegativeScore()
    {
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                new BigDecimal("-1"), ResultStatus.SCORED, new BigDecimal("12"),
                ResultStatus.SCORED, "corrector@example.com",
                OffsetDateTime.now(ZoneOffset.UTC)));
        assertThrows(IllegalArgumentException.class, () -> ResultCorrection.create(RESULT_ID,
                new BigDecimal("10"), ResultStatus.SCORED, new BigDecimal("-1"),
                ResultStatus.SCORED, "corrector@example.com",
                OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
