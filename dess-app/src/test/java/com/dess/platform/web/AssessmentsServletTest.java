package com.dess.platform.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;

class AssessmentsServletTest
{
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID OTHER_COURSE_ID = UUID.randomUUID();

    @Test
    void validCreateFormParses()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        AssessmentsServlet.ParsedAssessmentForm parsed = AssessmentsServlet.parseCreateForm(
                form("Final exam", "EXAM", "20", "90"), errors, Set.of(COURSE_ID));

        assertTrue(errors.isEmpty());
        assertEquals(COURSE_ID, parsed.courseId());
        assertEquals("Final exam", parsed.title());
        assertEquals(AssessmentType.EXAM, parsed.type());
        assertEquals(new BigDecimal("20"), parsed.maximumScore());
        assertEquals(90, parsed.durationMinutes());
    }

    @Test
    void missingTitleIsReportedAndKeepsTheOtherValues()
    {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, String> form = form("", "EXAM", "20", "90");

        assertNull(AssessmentsServlet.parseCreateForm(form, errors, Set.of(COURSE_ID)));
        assertEquals("Enter a title.", errors.get("title"));
        assertEquals("20", form.get("maximumScore"));
        assertEquals("90", form.get("durationMinutes"));
    }

    @Test
    void missingDurationIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "20", ""),
                errors, Set.of(COURSE_ID)));
        assertEquals("Enter a duration in minutes.", errors.get("durationMinutes"));
    }

    @Test
    void zeroAndNegativeDurationsAreRejected()
    {
        Map<String, String> zeroErrors = new LinkedHashMap<>();
        Map<String, String> negativeErrors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "20", "0"),
                zeroErrors, Set.of(COURSE_ID)));
        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "20", "-30"),
                negativeErrors, Set.of(COURSE_ID)));
        assertEquals("The duration must be greater than zero.", zeroErrors.get("durationMinutes"));
        assertEquals("The duration must be greater than zero.",
                negativeErrors.get("durationMinutes"));
    }

    @Test
    void nonNumericDurationIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(
                form("Final exam", "EXAM", "20", "ninety"), errors, Set.of(COURSE_ID)));
        assertEquals("Enter a whole number of minutes.", errors.get("durationMinutes"));
    }

    @Test
    void zeroMaximumScoreIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "0", "90"),
                errors, Set.of(COURSE_ID)));
        assertEquals("The maximum score must be greater than zero.", errors.get("maximumScore"));
    }

    @Test
    void maximumScoreWithThreeDecimalsIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "20.001", "90"),
                errors, Set.of(COURSE_ID)));
        assertEquals("The maximum score must have at most two decimal places.",
                errors.get("maximumScore"));
    }

    @Test
    void nonNumericMaximumScoreIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "twenty", "90"),
                errors, Set.of(COURSE_ID)));
        assertEquals("Enter a number for the maximum score.", errors.get("maximumScore"));
    }

    @Test
    void unknownTypeAndUnknownCourseAreRejected()
    {
        Map<String, String> typeErrors = new LinkedHashMap<>();
        Map<String, String> courseErrors = new LinkedHashMap<>();

        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "QUIZ", "20", "90"),
                typeErrors, Set.of(COURSE_ID)));
        assertNull(AssessmentsServlet.parseCreateForm(form("Final exam", "EXAM", "20", "90"),
                courseErrors, Set.of(OTHER_COURSE_ID)));
        assertEquals("Choose a valid type.", typeErrors.get("type"));
        assertEquals("Choose a known course.", courseErrors.get("courseId"));
    }

    @Test
    void filterKeepsOnlyTheMatchingType()
    {
        List<Assessment> visible = AssessmentsServlet.filterAssessments(
                List.of(exam(COURSE_ID), test(COURSE_ID)), "TEST", "");

        assertEquals(List.of("Midterm test"),
                visible.stream().map(Assessment::title).toList());
    }

    @Test
    void filterKeepsOnlyTheMatchingCourse()
    {
        List<Assessment> visible = AssessmentsServlet.filterAssessments(
                List.of(exam(COURSE_ID), exam(OTHER_COURSE_ID)), "", OTHER_COURSE_ID.toString());

        assertEquals(1, visible.size());
        assertEquals(OTHER_COURSE_ID, visible.get(0).courseId());
    }

    @Test
    void filterCombinesTypeAndCourse()
    {
        List<Assessment> visible = AssessmentsServlet.filterAssessments(
                List.of(exam(COURSE_ID), test(COURSE_ID), exam(OTHER_COURSE_ID)),
                "EXAM", COURSE_ID.toString());

        assertEquals(List.of("Final exam"),
                visible.stream().map(Assessment::title).toList());
    }

    @Test
    void unknownFilterValuesMatchNothing()
    {
        List<Assessment> assessments = List.of(exam(COURSE_ID));

        assertTrue(AssessmentsServlet.filterAssessments(assessments, "QUIZ", "").isEmpty());
        assertTrue(AssessmentsServlet.filterAssessments(assessments, "", "not-a-uuid").isEmpty());
    }

    @Test
    void emptyFiltersShowEverything()
    {
        List<Assessment> assessments = List.of(exam(COURSE_ID), test(OTHER_COURSE_ID));

        assertEquals(2, AssessmentsServlet.filterAssessments(assessments, "", "").size());
    }

    private static Map<String, String> form(String title, String type, String maximumScore,
                                            String durationMinutes)
    {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("courseId", COURSE_ID.toString());
        form.put("title", title);
        form.put("type", type);
        form.put("maximumScore", maximumScore);
        form.put("durationMinutes", durationMinutes);
        return form;
    }

    private static Assessment exam(UUID courseId)
    {
        return Assessment.create(courseId, "Final exam", AssessmentType.EXAM,
                new BigDecimal("20"), 90);
    }

    private static Assessment test(UUID courseId)
    {
        return Assessment.create(courseId, "Midterm test", AssessmentType.TEST,
                new BigDecimal("10"), 45);
    }
}
