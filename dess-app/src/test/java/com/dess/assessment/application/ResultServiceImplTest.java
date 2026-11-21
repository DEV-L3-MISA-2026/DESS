package com.dess.assessment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dess.academic.application.EnrollStudentCommand;
import com.dess.academic.application.ProgramEnrollmentService;
import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.assessment.application.impl.EligibilityServiceImpl;
import com.dess.assessment.application.impl.ResultServiceImpl;
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.application.spi.ResultRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultStatus;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.assessment.exception.DuplicateResultException;
import com.dess.assessment.exception.IneligibleCandidateException;
import com.dess.assessment.exception.InvalidResultException;
import com.dess.assessment.exception.ResultNotFoundException;
import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;

class ResultServiceImplTest
{
    private static final long STUDENT = 1L;
    private static final long UNKNOWN_STUDENT = 2L;

    private ResultServiceImpl service;
    private Assessment assessment;

    @BeforeEach
    void setUp()
    {
        InMemoryAssessmentRepository assessments = new InMemoryAssessmentRepository();
        assessment = Assessment.create(UUID.randomUUID(), "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"));
        assessments.save(assessment);
        InMemoryResultRepository results = new InMemoryResultRepository();
        EligibilityService eligibilityService = new EligibilityServiceImpl(assessments,
                new StubPeopleService(), new StubEnrollmentService());
        service = new ResultServiceImpl(results, assessments, eligibilityService);
    }

    @Test
    void recordsAScoredResult()
    {
        Result recorded = service.recordResult(new RecordResultCommand(assessment.id(),
                UUID.randomUUID(), STUDENT, new BigDecimal("15.50"), ResultStatus.SCORED,
                "professor@example.com"));

        assertEquals(new BigDecimal("15.50"), recorded.score());
        assertEquals(ResultStatus.SCORED, recorded.status());
        assertEquals(recorded, service.findById(recorded.id()).orElseThrow());
        assertEquals(recorded, service.findByAssessmentAndStudent(assessment.id(), STUDENT)
                .orElseThrow());
        assertEquals(1, service.listByAssessment(assessment.id()).size());
        assertEquals(1, service.listByStudent(STUDENT).size());
    }

    @Test
    void recordsAnAbsentResultWithoutAScore()
    {
        Result recorded = service.recordResult(new RecordResultCommand(assessment.id(), null,
                STUDENT, null, ResultStatus.ABSENT, "professor@example.com"));

        assertNull(recorded.score());
        assertEquals(ResultStatus.ABSENT, recorded.status());
    }

    @Test
    void rejectsAScoreAboveTheMaximum()
    {
        assertThrows(InvalidResultException.class, () -> service.recordResult(
                new RecordResultCommand(assessment.id(), null, STUDENT,
                        new BigDecimal("20.01"), ResultStatus.SCORED, "professor@example.com")));
    }

    @Test
    void acceptsAScoreEqualToTheMaximum()
    {
        Result recorded = service.recordResult(new RecordResultCommand(assessment.id(), null,
                STUDENT, new BigDecimal("20"), ResultStatus.SCORED, "professor@example.com"));

        assertEquals(new BigDecimal("20"), recorded.score());
    }

    @Test
    void rejectsAResultForAnUnknownAssessment()
    {
        assertThrows(AssessmentNotFoundException.class, () -> service.recordResult(
                new RecordResultCommand(UUID.randomUUID(), null, STUDENT,
                        new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com")));
    }

    @Test
    void rejectsAResultForAnIneligibleStudent()
    {
        assertThrows(IneligibleCandidateException.class, () -> service.recordResult(
                new RecordResultCommand(assessment.id(), null, UNKNOWN_STUDENT,
                        new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com")));
    }

    @Test
    void rejectsASecondResultForTheSameAssessmentAndStudent()
    {
        service.recordResult(new RecordResultCommand(assessment.id(), null, STUDENT,
                new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com"));

        assertThrows(DuplicateResultException.class, () -> service.recordResult(
                new RecordResultCommand(assessment.id(), UUID.randomUUID(), STUDENT,
                        new BigDecimal("12"), ResultStatus.SCORED, "professor@example.com")));
    }

    @Test
    void correctsAResultAndKeepsTheEntryAuditFields()
    {
        Result recorded = service.recordResult(new RecordResultCommand(assessment.id(), null,
                STUDENT, new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com"));

        Result corrected = service.correctResult(recorded.id(),
                new CorrectResultCommand(new BigDecimal("12"), ResultStatus.SCORED));

        assertEquals(new BigDecimal("12"), corrected.score());
        assertEquals("professor@example.com", corrected.enteredBy());
        assertEquals(recorded.enteredAt(), corrected.enteredAt());
        assertTrue(!corrected.updatedAt().isBefore(recorded.enteredAt()));
        assertEquals(corrected, service.findById(recorded.id()).orElseThrow());
    }

    @Test
    void rejectsACorrectionAboveTheMaximum()
    {
        Result recorded = service.recordResult(new RecordResultCommand(assessment.id(), null,
                STUDENT, new BigDecimal("10"), ResultStatus.SCORED, "professor@example.com"));

        assertThrows(InvalidResultException.class, () -> service.correctResult(recorded.id(),
                new CorrectResultCommand(new BigDecimal("21"), ResultStatus.SCORED)));
    }

    @Test
    void rejectsCorrectionOfAnUnknownResult()
    {
        assertThrows(ResultNotFoundException.class, () -> service.correctResult(
                UUID.randomUUID(),
                new CorrectResultCommand(new BigDecimal("12"), ResultStatus.SCORED)));
    }

    private static class InMemoryAssessmentRepository implements AssessmentRepository
    {
        private final Map<UUID, Assessment> assessments = new HashMap<>();

        @Override
        public Assessment save(Assessment assessment)
        {
            assessments.put(assessment.id(), assessment);
            return assessment;
        }

        @Override
        public Optional<Assessment> findById(UUID id)
        {
            return Optional.ofNullable(assessments.get(id));
        }

        @Override
        public List<Assessment> findAll()
        {
            return new ArrayList<>(assessments.values());
        }

        @Override
        public List<Assessment> findByCourseId(UUID courseId)
        {
            return assessments.values().stream()
                    .filter(assessment -> assessment.courseId().equals(courseId))
                    .toList();
        }
    }

    private static class InMemoryResultRepository implements ResultRepository
    {
        private final Map<UUID, Result> results = new HashMap<>();

        @Override
        public Result save(Result result)
        {
            results.put(result.id(), result);
            return result;
        }

        @Override
        public Optional<Result> findById(UUID id)
        {
            return Optional.ofNullable(results.get(id));
        }

        @Override
        public Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId)
        {
            return results.values().stream()
                    .filter(result -> result.assessmentId().equals(assessmentId)
                            && result.studentPersonId() == studentPersonId)
                    .findFirst();
        }

        @Override
        public List<Result> findByAssessment(UUID assessmentId)
        {
            return results.values().stream()
                    .filter(result -> result.assessmentId().equals(assessmentId))
                    .toList();
        }

        @Override
        public List<Result> findByStudent(long studentPersonId)
        {
            return results.values().stream()
                    .filter(result -> result.studentPersonId() == studentPersonId)
                    .toList();
        }

        @Override
        public boolean existsByAssessmentAndStudent(UUID assessmentId, long studentPersonId)
        {
            return findByAssessmentAndStudent(assessmentId, studentPersonId).isPresent();
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
            return List.of();
        }

        @Override
        public boolean studentExists(long id)
        {
            return id == STUDENT;
        }
    }

    private static class StubEnrollmentService implements ProgramEnrollmentService
    {
        @Override
        public ProgramEnrollment enroll(EnrollStudentCommand command)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public ProgramEnrollment complete(long enrollmentId, LocalDate endDate)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public ProgramEnrollment withdraw(long enrollmentId, LocalDate endDate)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public List<ProgramEnrollment> listByStudent(long studentId)
        {
            if (studentId == STUDENT)
            {
                return List.of(new ProgramEnrollment(1L, studentId, 10L,
                        LocalDate.of(2026, 9, 1), null, EnrollmentStatus.ACTIVE));
            }
            return List.of();
        }
    }
}
