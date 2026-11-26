package com.dess.platform.web;

import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentPresentationTest
{
    private static final UUID KNOWN_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void labelsKeepTheAssessmentTitle()
    {
        Assessment assessment = assessment(KNOWN_ID, "Algorithms midterm");

        Map<UUID, String> labels = AssessmentPresentation.labels(List.of(assessment));

        assertEquals("Algorithms midterm", labels.get(KNOWN_ID));
        assertEquals("Algorithms midterm",
                AssessmentPresentation.label(KNOWN_ID, labels));
    }

    @Test
    void labelExplainsAnUnknownAssessment()
    {
        Map<UUID, String> labels = AssessmentPresentation.labels(
                List.of(assessment(KNOWN_ID, "Algorithms midterm")));

        assertEquals(AssessmentPresentation.UNKNOWN_LABEL,
                AssessmentPresentation.label(OTHER_ID, labels));
    }

    @Test
    void labelRejectsNullArguments()
    {
        assertThrows(NullPointerException.class,
                () -> AssessmentPresentation.label(null, Map.of()));
        assertThrows(NullPointerException.class,
                () -> AssessmentPresentation.label(KNOWN_ID, null));
    }

    @Test
    void typeLabelsNameEachKind()
    {
        assertEquals("Exam", AssessmentPresentation.typeLabel(AssessmentType.EXAM));
        assertEquals("Test", AssessmentPresentation.typeLabel(AssessmentType.TEST));
        assertEquals("Practical", AssessmentPresentation.typeLabel(AssessmentType.PRACTICAL));
    }

    @Test
    void typeLabelRejectsNull()
    {
        assertThrows(NullPointerException.class,
                () -> AssessmentPresentation.typeLabel(null));
    }

    private static Assessment assessment(UUID id, String title)
    {
        return new Assessment(id, UUID.randomUUID(), title, AssessmentType.EXAM,
                new BigDecimal("20.00"), true,
                OffsetDateTime.of(2026, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC));
    }
}
