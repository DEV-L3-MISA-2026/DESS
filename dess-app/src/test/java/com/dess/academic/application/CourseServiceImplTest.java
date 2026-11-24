package com.dess.academic.application;

import com.dess.academic.application.impl.CourseServiceImpl;
import com.dess.academic.domain.Course;
import com.dess.academic.exception.CourseNotFoundException;
import com.dess.academic.exception.DuplicateCourseCodeException;
import com.dess.academic.infrastructure.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires des règles du catalogue des cours. */
class CourseServiceImplTest {
    private InMemoryCourseRepository repository;
    private CourseServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepository();
        service = new CourseServiceImpl(repository);
    }

    @Test
    void createsCourseWithNormalisedCode() {
        Course course = service.create(new CreateCourseCommand(" inf101 ", "Algorithmique"));

        assertEquals("INF101", course.code());
        assertEquals("Algorithmique", course.title());
        assertTrue(course.active());
        assertTrue(repository.existsByCode("INF101"));
    }

    @Test
    void rejectsDuplicateCode() {
        service.create(new CreateCourseCommand("INF101", "Algorithmique"));

        assertThrows(DuplicateCourseCodeException.class,
                () -> service.create(new CreateCourseCommand("inf101", "Autre cours")));
    }

    @Test
    void rejectsMissingCommandOrBlankFields() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        assertThrows(IllegalArgumentException.class, () -> new CreateCourseCommand("  ", "Titre"));
        assertThrows(IllegalArgumentException.class, () -> new CreateCourseCommand("CODE", "  "));
    }

    @Test
    void getsCourseByIdAndFailsWhenMissing() {
        Course created = service.create(new CreateCourseCommand("INF101", "Algorithmique"));

        assertEquals(created, service.get(created.id()));
        assertThrows(IllegalArgumentException.class, () -> service.get(null));
        assertThrows(CourseNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void listsCourses() {
        service.create(new CreateCourseCommand("B", "Deuxième"));
        service.create(new CreateCourseCommand("A", "Premier"));

        assertEquals(List.of("A", "B"), service.list().stream().map(Course::code).toList());
    }

    private static class InMemoryCourseRepository implements CourseRepository {
        private final Map<UUID, Course> courses = new LinkedHashMap<>();

        @Override
        public Course save(Course course) {
            courses.put(course.id(), course);
            return course;
        }

        @Override
        public Optional<Course> findById(UUID id) {
            return Optional.ofNullable(courses.get(id));
        }

        @Override
        public Optional<Course> findByCode(String code) {
            return courses.values().stream().filter(c -> c.code().equals(code)).findFirst();
        }

        @Override
        public boolean existsByCode(String code) {
            return findByCode(code).isPresent();
        }

        @Override
        public List<Course> findAll() {
            return courses.values().stream().sorted((a, b) -> a.code().compareTo(b.code())).toList();
        }
    }
}
