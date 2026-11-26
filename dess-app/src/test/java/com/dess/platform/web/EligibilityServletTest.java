package com.dess.platform.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dess.academic.application.EnrollStudentCommand;
import com.dess.academic.application.ProgramEnrollmentService;
import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.assessment.application.EligibilityReason;
import com.dess.assessment.application.EligibilityService;
import com.dess.assessment.application.impl.EligibilityServiceImpl;
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import com.dess.assessment.domain.ExamSession;
import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import com.dess.platform.web.view.CandidateRow;

class EligibilityServletTest
{
    private static final long ELIGIBLE_STUDENT = 1L;
    private static final long UNKNOWN_STUDENT = 2L;
    private static final long NOT_ENROLLED_STUDENT = 3L;

    private Assessment assessment;
    private EligibilityService eligibilityService;
    private PeopleService peopleService;

    @BeforeEach
    void setUp()
    {
        InMemoryAssessmentRepository assessments = new InMemoryAssessmentRepository();
        assessment = Assessment.create(UUID.randomUUID(), "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"), 90);
        assessments.save(assessment);
        eligibilityService = new EligibilityServiceImpl(assessments,
                new StubPeopleService(), new StubEnrollmentService());
        peopleService = new StubPeopleService();
    }

    @Test
    void candidatesComeFromTheSessionsOfTheAssessment()
    {
        ExamSession first = sessionFor(assessment.id()).withCandidates(
                List.of(ELIGIBLE_STUDENT, NOT_ENROLLED_STUDENT));
        ExamSession second = sessionFor(assessment.id()).withCandidates(
                List.of(ELIGIBLE_STUDENT));
        ExamSession other = sessionFor(UUID.randomUUID()).withCandidates(
                List.of(UNKNOWN_STUDENT));

        EligibilityServlet.CandidateIds candidates = EligibilityServlet.resolveCandidateIds(
                List.of(first, second, other), assessment.id(), "");

        assertEquals(List.of(ELIGIBLE_STUDENT, NOT_ENROLLED_STUDENT), candidates.ids());
        assertTrue(candidates.errors().isEmpty());
        assertEquals(2, candidates.sessionCount());
    }

    @Test
    void explicitStudentListIsUsedWhenNoSessionExists()
    {
        EligibilityServlet.CandidateIds candidates = EligibilityServlet.resolveCandidateIds(
                List.of(), assessment.id(), "1, 3");

        assertEquals(List.of(ELIGIBLE_STUDENT, NOT_ENROLLED_STUDENT), candidates.ids());
        assertTrue(candidates.errors().isEmpty());
        assertEquals(0, candidates.sessionCount());
    }

    @Test
    void explicitIdsAreAddedToTheSessionCandidates()
    {
        ExamSession session = sessionFor(assessment.id()).withCandidates(
                List.of(ELIGIBLE_STUDENT));

        EligibilityServlet.CandidateIds candidates = EligibilityServlet.resolveCandidateIds(
                List.of(session), assessment.id(), "3, 1");

        assertEquals(List.of(ELIGIBLE_STUDENT, NOT_ENROLLED_STUDENT), candidates.ids());
    }

    @Test
    void invalidExplicitIdsAreReported()
    {
        EligibilityServlet.CandidateIds candidates = EligibilityServlet.resolveCandidateIds(
                List.of(), assessment.id(), "1, abc");

        assertEquals(List.of(ELIGIBLE_STUDENT), candidates.ids());
        assertEquals(1, candidates.errors().size());
    }

    @Test
    void rowsMatchTheServiceOutputForASeededCase()
    {
        List<CandidateRow> rows = EligibilityServlet.buildRows(
                List.of(ELIGIBLE_STUDENT, UNKNOWN_STUDENT, NOT_ENROLLED_STUDENT),
                assessment.id(), eligibilityService, peopleService);

        assertEquals(3, rows.size());

        CandidateRow eligible = rows.get(0);
        assertTrue(eligible.isEligible());
        assertTrue(eligible.getReasons().isEmpty());
        assertEquals("", eligible.getReasonMessage());
        assertEquals("Eligible", eligible.getEligibility().getLabel());
        assertEquals("STU-001", eligible.getRegistrationNumber());
        assertEquals("Ada Lovelace", eligible.getName());

        CandidateRow unknown = rows.get(1);
        assertFalse(unknown.isEligible());
        assertEquals(List.of(EligibilityReason.STUDENT_NOT_FOUND), unknown.getReasons());
        assertEquals("student not found: 2", unknown.getReasonMessage());
        assertEquals("Unknown person", unknown.getName());

        CandidateRow notEnrolled = rows.get(2);
        assertFalse(notEnrolled.isEligible());
        assertEquals(List.of(EligibilityReason.NO_ACTIVE_PROGRAM_ENROLLMENT),
                notEnrolled.getReasons());
        assertEquals("student has no active program enrollment",
                notEnrolled.getReasonMessage());
    }

    private static ExamSession sessionFor(UUID assessmentId)
    {
        return ExamSession.create(assessmentId, UUID.randomUUID(),
                OffsetDateTime.of(2026, 11, 28, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 28, 11, 0, 0, 0, ZoneOffset.UTC));
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
            if (id == ELIGIBLE_STUDENT)
            {
                return Optional.of(new Person(1L, "ada@example.test", "Lovelace", "Ada", true));
            }
            if (id == NOT_ENROLLED_STUDENT)
            {
                return Optional.of(new Person(3L, "grace@example.test", "Hopper", "Grace", true));
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
            return List.of(new StudentProfile(ELIGIBLE_STUDENT, "STU-001"),
                    new StudentProfile(NOT_ENROLLED_STUDENT, "STU-003"));
        }

        @Override
        public boolean studentExists(long id)
        {
            return id == ELIGIBLE_STUDENT || id == NOT_ENROLLED_STUDENT;
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
            if (studentId == ELIGIBLE_STUDENT)
            {
                return List.of(new ProgramEnrollment(1L, studentId, 10L,
                        LocalDate.of(2026, 9, 1), null, EnrollmentStatus.ACTIVE));
            }
            if (studentId == NOT_ENROLLED_STUDENT)
            {
                return List.of(new ProgramEnrollment(2L, studentId, 10L,
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1),
                        EnrollmentStatus.WITHDRAWN));
            }
            return List.of();
        }
    }
}
