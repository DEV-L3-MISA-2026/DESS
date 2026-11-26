package com.dess.assessment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dess.academic.application.CourseService;
import com.dess.academic.application.CreateCourseCommand;
import com.dess.academic.domain.Course;
import com.dess.academic.exception.CourseNotFoundException;
import com.dess.assessment.application.impl.AssessmentServiceImpl;
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.assessment.exception.InvalidAssessmentException;

class AssessmentServiceImplTest
{
    private InMemoryAssessmentRepository repository;
    private AssessmentServiceImpl service;
    private UUID courseId;

    @BeforeEach
    void setUp()
    {
        repository = new InMemoryAssessmentRepository();
        Course course = Course.create("MATH101", "Mathematics");
        courseId = course.id();
        service = new AssessmentServiceImpl(repository, new StubCourseService(course));
    }

    @Test
    void createsAnAssessmentForAnExistingCourse()
    {
        Assessment created = service.create(new CreateAssessmentCommand(courseId, "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"), 90));

        assertEquals(courseId, created.courseId());
        assertEquals("Final exam", created.title());
        assertEquals(90, created.durationMinutes());
        assertTrue(created.active());
        assertEquals(created, service.get(created.id()));
        assertEquals(1, service.listByCourse(courseId).size());
        assertEquals(1, service.list().size());
    }

    @Test
    void rejectsAnAssessmentForAnUnknownCourse()
    {
        UUID unknownCourseId = UUID.randomUUID();

        assertThrows(InvalidAssessmentException.class, () -> service.create(
                new CreateAssessmentCommand(unknownCourseId, "Final exam",
                        AssessmentType.EXAM, new BigDecimal("20"), 90)));
    }

    @Test
    void rejectsAMissingDuration()
    {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new CreateAssessmentCommand(courseId, "Final exam",
                        AssessmentType.EXAM, new BigDecimal("20"), null)));
    }

    @Test
    void rejectsAZeroDuration()
    {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new CreateAssessmentCommand(courseId, "Final exam",
                        AssessmentType.EXAM, new BigDecimal("20"), 0)));
    }

    @Test
    void rejectsANegativeDuration()
    {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new CreateAssessmentCommand(courseId, "Final exam",
                        AssessmentType.EXAM, new BigDecimal("20"), -30)));
    }

    @Test
    void rejectsAnUnknownAssessmentLookup()
    {
        assertThrows(AssessmentNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void deactivatesAnAssessment()
    {
        Assessment created = service.create(new CreateAssessmentCommand(courseId, "Final exam",
                AssessmentType.EXAM, new BigDecimal("20"), 90));

        Assessment deactivated = service.deactivate(created.id());

        assertFalse(deactivated.active());
        assertEquals(created.id(), deactivated.id());
        assertFalse(service.get(created.id()).active());
    }

    @Test
    void rejectsDeactivationOfAnUnknownAssessment()
    {
        assertThrows(AssessmentNotFoundException.class,
                () -> service.deactivate(UUID.randomUUID()));
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

    private static class StubCourseService implements CourseService
    {
        private final Course course;

        StubCourseService(Course course)
        {
            this.course = course;
        }

        @Override
        public Course create(CreateCourseCommand command)
        {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Course get(UUID id)
        {
            if (!course.id().equals(id))
            {
                throw new CourseNotFoundException("course not found: " + id);
            }
            return course;
        }

        @Override
        public List<Course> list()
        {
            return List.of(course);
        }
    }
}
