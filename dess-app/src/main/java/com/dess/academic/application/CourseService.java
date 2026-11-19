package com.dess.academic.application;

import com.dess.academic.domain.Course;
import com.dess.academic.infrastructure.CourseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class CourseService 
{

    @Inject
    CourseRepository repository;

    public Course create(String code, String title) 
    {
        String normalized = Course.normalizeCode(code);
        if (repository.existsByCode(normalized)) 
        {
            throw new DuplicateCourseCodeException(normalized);
        }
        Course course = new Course(normalized, title);
        repository.save(course);
        return course;
    }

    public Course get(UUID id) 
    {
        return repository.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
    }

    public List<Course> list() 
    { 
        return repository.findAll(); 
    }

    public static class DuplicateCourseCodeException extends RuntimeException 
    {
        public DuplicateCourseCodeException(String code) 
        {
            super("A course with code '" + code + "' already exists");
        }
    }

    public static class CourseNotFoundException extends RuntimeException 
    {
        public CourseNotFoundException(UUID id) 
        { 
            super("Course not found: " + id); 
        }
    }
}