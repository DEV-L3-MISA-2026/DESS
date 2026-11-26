package com.dess.assessment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;

class EligibilityServiceImplTest
{
    private static final long ELIGIBLE_STUDENT = 1L;
    private static final long UNKNOWN_STUDENT = 2L;
    private static final long NOT_ENROLLED_STUDENT = 3L;

    private EligibilityServiceImpl service;
    private Assessment assessment;

    @BeforeEach
    void setUp()
    {
        InMemoryAssessmentRepository assessments = new InMemoryAssessmentRepository();
        assessment = Assessment.create(UUID.randomUUID(), "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"), 90);
        assessments.save(assessment);
        service = new EligibilityServiceImpl(assessments,
                new StubPeopleService(), new StubEnrollmentService());
    }

    @Test
    void eligibleStudentHasAnActiveEnrollment()
    {
        EligibilityService.Eligibility eligibility = service.check(assessment.id(), ELIGIBLE_STUDENT);

        assertTrue(eligibility.eligible());
        assertTrue(eligibility.reasons().isEmpty());
    }

    @Test
    void unknownStudentIsNotEligible()
    {
        EligibilityService.Eligibility eligibility = service.check(assessment.id(), UNKNOWN_STUDENT);

        assertFalse(eligibility.eligible());
        assertEquals(List.of(EligibilityReason.STUDENT_NOT_FOUND), eligibility.reasons());
    }

    @Test
    void studentWithoutAnActiveEnrollmentIsNotEligible()
    {
        EligibilityService.Eligibility eligibility =
                service.check(assessment.id(), NOT_ENROLLED_STUDENT);

        assertFalse(eligibility.eligible());
        assertEquals(List.of(EligibilityReason.NO_ACTIVE_PROGRAM_ENROLLMENT),
                eligibility.reasons());
    }

    @Test
    void inactiveAssessmentMakesEveryStudentIneligible()
    {
        Assessment inactive = assessment.deactivated();
        InMemoryAssessmentRepository assessments = new InMemoryAssessmentRepository();
        assessments.save(inactive);
        EligibilityServiceImpl inactiveService = new EligibilityServiceImpl(assessments,
                new StubPeopleService(), new StubEnrollmentService());

        EligibilityService.Eligibility eligibility =
                inactiveService.check(inactive.id(), ELIGIBLE_STUDENT);

        assertFalse(eligibility.eligible());
        assertEquals(List.of(EligibilityReason.ASSESSMENT_INACTIVE), eligibility.reasons());
    }

    @Test
    void unknownAssessmentThrows()
    {
        assertThrows(AssessmentNotFoundException.class,
                () -> service.check(UUID.randomUUID(), ELIGIBLE_STUDENT));
    }

    @Test
    void eligibleStudentsFiltersTheList()
    {
        List<Long> eligible = service.eligibleStudents(assessment.id(),
                List.of(ELIGIBLE_STUDENT, UNKNOWN_STUDENT, NOT_ENROLLED_STUDENT));

        assertEquals(List.of(ELIGIBLE_STUDENT), eligible);
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
