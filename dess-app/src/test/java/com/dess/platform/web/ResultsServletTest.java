package com.dess.platform.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dess.assessment.application.CorrectResultCommand;
import com.dess.assessment.application.RecordResultCommand;
import com.dess.assessment.application.ResultService;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultCorrection;
import com.dess.assessment.domain.ResultStatus;
import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import com.dess.platform.web.view.ResultEntryRow;

class ResultsServletTest
{
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final BigDecimal MAXIMUM = new BigDecimal("20");

    @Test
    void validEntryParses()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        ResultsServlet.ParsedEntryForm parsed = ResultsServlet.parseEntryForm(
                form("1", "SCORED", "15.50"), errors, MAXIMUM);

        assertTrue(errors.isEmpty());
        assertEquals(ASSESSMENT_ID, parsed.assessmentId());
        assertEquals(1L, parsed.studentPersonId());
        assertEquals(new BigDecimal("15.50"), parsed.score());
        assertEquals(ResultStatus.SCORED, parsed.status());
    }

    @Test
    void markAboveTheMaximumIsRejectedAndKeepsTheEnteredValue()
    {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, String> form = form("1", "SCORED", "20.01");

        assertNull(ResultsServlet.parseEntryForm(form, errors, MAXIMUM));
        assertEquals("The mark must be at most 20.", errors.get("score-1"));
        assertEquals("20.01", form.get("score"));
    }

    @Test
    void markEqualToTheMaximumIsAccepted()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        ResultsServlet.ParsedEntryForm parsed = ResultsServlet.parseEntryForm(
                form("1", "SCORED", "20"), errors, MAXIMUM);

        assertTrue(errors.isEmpty());
        assertEquals(new BigDecimal("20"), parsed.score());
    }

    @Test
    void absentResultWithAMarkIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(ResultsServlet.parseEntryForm(form("1", "ABSENT", "12"), errors, MAXIMUM));
        assertEquals("Remove the mark: an absent or excused result carries no mark.",
                errors.get("score-1"));
    }

    @Test
    void scoredResultWithoutAMarkIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(ResultsServlet.parseEntryForm(form("1", "SCORED", ""), errors, MAXIMUM));
        assertEquals("Enter a mark.", errors.get("score-1"));
    }

    @Test
    void negativeAndNonNumericMarksAreRejected()
    {
        Map<String, String> negativeErrors = new LinkedHashMap<>();
        Map<String, String> textErrors = new LinkedHashMap<>();

        assertNull(ResultsServlet.parseEntryForm(form("1", "SCORED", "-1"), negativeErrors,
                MAXIMUM));
        assertNull(ResultsServlet.parseEntryForm(form("1", "SCORED", "fifteen"), textErrors,
                MAXIMUM));
        assertEquals("The mark must be at least zero.", negativeErrors.get("score-1"));
        assertEquals("Enter a number for the mark.", textErrors.get("score-1"));
    }

    @Test
    void unknownStatusIsRejected()
    {
        Map<String, String> errors = new LinkedHashMap<>();

        assertNull(ResultsServlet.parseEntryForm(form("1", "PASSED", ""), errors, MAXIMUM));
        assertEquals("Choose a valid status.", errors.get("status-1"));
    }

    @Test
    void entryRowsShowOnlyTheStoredMark()
    {
        UUID sessionId = UUID.randomUUID();
        Result stored = Result.create(ASSESSMENT_ID, sessionId, 1L,
                new BigDecimal("14"), ResultStatus.SCORED, "Fabien Halaby");
        Result corrected = stored.corrected(new BigDecimal("16"), ResultStatus.SCORED);
        ResultCorrection trail = ResultCorrection.create(stored.id(),
                new BigDecimal("14"), ResultStatus.SCORED, new BigDecimal("16"),
                ResultStatus.SCORED, "Fabien Halaby", corrected.updatedAt());
        StubResultService results = new StubResultService(corrected, List.of(trail));

        ExamSession session = ExamSession.create(ASSESSMENT_ID, UUID.randomUUID(),
                OffsetDateTime.of(2026, 11, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 28, 11, 0, 0, 0, ZoneOffset.UTC))
                .withCandidates(List.of(1L, 2L));

        List<ResultEntryRow> rows = ResultsServlet.buildEntryRows(session, ASSESSMENT_ID,
                results, new StubPeopleService());

        assertEquals(2, rows.size());

        ResultEntryRow entered = rows.get(0);
        assertEquals("16", entered.getStoredScore());
        assertEquals("Scored", entered.getStatus().getLabel());
        assertEquals("Ada Lovelace", entered.getName());
        assertEquals("STU-001", entered.getRegistrationNumber());
        assertEquals(1, entered.getCorrections().size());
        assertEquals("14", entered.getCorrections().get(0).getPreviousScore());
        assertEquals("16", entered.getCorrections().get(0).getNewScore());
        assertEquals("Fabien Halaby", entered.getCorrections().get(0).getCorrectedBy());

        ResultEntryRow missing = rows.get(1);
        assertEquals("—", missing.getStoredScore());
        assertEquals("Not entered", missing.getStatus().getLabel());
        assertEquals("", missing.getResultId());
        assertTrue(missing.getCorrections().isEmpty());
    }

    private static Map<String, String> form(String studentPersonId, String status, String score)
    {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("assessmentId", ASSESSMENT_ID.toString());
        form.put("sessionId", UUID.randomUUID().toString());
        form.put("studentPersonId", studentPersonId);
        form.put("resultId", "");
        form.put("score", score);
        form.put("status", status);
        return form;
    }

    private static class StubResultService implements ResultService
    {
        private final Result stored;
        private final List<ResultCorrection> corrections;

        StubResultService(Result stored, List<ResultCorrection> corrections)
        {
            this.stored = stored;
            this.corrections = corrections;
        }

        @Override
        public Result recordResult(RecordResultCommand command)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Result correctResult(UUID resultId, CorrectResultCommand command)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Optional<Result> findById(UUID id)
        {
            return stored.id().equals(id) ? Optional.of(stored) : Optional.empty();
        }

        @Override
        public Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId)
        {
            if (stored.assessmentId().equals(assessmentId)
                    && stored.studentPersonId() == studentPersonId)
            {
                return Optional.of(stored);
            }
            return Optional.empty();
        }

        @Override
        public List<Result> listByAssessment(UUID assessmentId)
        {
            return List.of(stored);
        }

        @Override
        public List<Result> listByStudent(long studentPersonId)
        {
            return List.of(stored);
        }

        @Override
        public List<ResultCorrection> correctionsFor(UUID resultId)
        {
            return corrections;
        }

        @Override
        public ResultDetail detail(UUID id)
        {
            return new ResultDetail(stored, corrections);
        }
    }

    private static class StubPeopleService implements PeopleService
    {
        @Override
        public StudentProfile createStudent(CreateStudentCommand command)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public TeacherProfile createTeacher(CreateTeacherCommand command)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Optional<Person> findById(long id)
        {
            if (id == 1L)
            {
                return Optional.of(new Person(1L, "ada@example.test", "Lovelace", "Ada", true));
            }
            if (id == 2L)
            {
                return Optional.of(new Person(2L, "grace@example.test", "Hopper", "Grace", true));
            }
            return Optional.empty();
        }

        @Override
        public Optional<Person> findByEmail(String email)
        {
            return Optional.empty();
        }

        @Override
        public List<StudentProfile> listStudents()
        {
            return List.of(new StudentProfile(1L, "STU-001"),
                    new StudentProfile(2L, "STU-002"));
        }

        @Override
        public boolean studentExists(long id)
        {
            return id == 1L || id == 2L;
        }
    }
}
