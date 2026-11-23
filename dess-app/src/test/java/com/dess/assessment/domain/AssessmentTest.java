package com.dess.assessment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AssessmentTest
{
    private static final UUID COURSE_ID = UUID.randomUUID();

    @Test
    void createsAnActiveAssessment()
    {
        Assessment assessment = Assessment.create(COURSE_ID, "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"));

        assertNotNull(assessment.id());
        assertEquals(COURSE_ID, assessment.courseId());
        assertEquals("Final exam", assessment.title());
        assertEquals(AssessmentType.EXAM, assessment.type());
        assertEquals(new BigDecimal("20"), assessment.maximumScore());
        assertTrue(assessment.active());
        assertNotNull(assessment.createdAt());
    }

    @Test
    void convertsCreationTimeToUtc()
    {
        Assessment assessment = new Assessment(UUID.randomUUID(), COURSE_ID, "Test",
                AssessmentType.TEST, new BigDecimal("10"), true,
                OffsetDateTime.parse("2026-11-21T12:00:00+03:00"));

        assertEquals(ZoneOffset.UTC, assessment.createdAt().getOffset());
        assertEquals(9, assessment.createdAt().getHour());
    }

    @Test
    void deactivationKeepsTheIdentity()
    {
        Assessment assessment = Assessment.create(COURSE_ID, "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"));

        Assessment deactivated = assessment.deactivated();

        assertEquals(assessment.id(), deactivated.id());
        assertFalse(deactivated.active());
        assertEquals(assessment.title(), deactivated.title());
        assertEquals(assessment.maximumScore(), deactivated.maximumScore());
    }

    @Test
    void rejectsABlankTitle()
    {
        assertThrows(IllegalArgumentException.class, () -> Assessment.create(COURSE_ID, "  ",
                AssessmentType.EXAM, new BigDecimal("20")));
    }

    @Test
    void rejectsATitleThatIsTooLong()
    {
        assertThrows(IllegalArgumentException.class, () -> Assessment.create(COURSE_ID,
                "x".repeat(201), AssessmentType.EXAM, new BigDecimal("20")));
    }

    @Test
    void rejectsAMissingType()
    {
        assertThrows(NullPointerException.class, () -> Assessment.create(COURSE_ID,
                "Final exam", null, new BigDecimal("20")));
    }

    @Test
    void rejectsAZeroMaximumScore()
    {
        assertThrows(IllegalArgumentException.class, () -> Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, BigDecimal.ZERO));
    }

    @Test
    void rejectsANegativeMaximumScore()
    {
        assertThrows(IllegalArgumentException.class, () -> Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, new BigDecimal("-5")));
    }

    @Test
    void rejectsAMaximumScoreWithMoreThanTwoDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> Assessment.create(COURSE_ID,
                "Final exam", AssessmentType.EXAM, new BigDecimal("20.001")));
    }

    @Test
    void rejectsAMissingCourse()
    {
        assertThrows(NullPointerException.class, () -> Assessment.create(null, "Final exam",
                AssessmentType.EXAM, new BigDecimal("20")));
    }
}
