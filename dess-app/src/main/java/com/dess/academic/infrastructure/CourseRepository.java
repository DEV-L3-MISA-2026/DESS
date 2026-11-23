package com.dess.academic.infrastructure;

import com.dess.academic.domain.Course;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port d'accès aux données du catalogue des cours. */
public interface CourseRepository {
    Course save(Course course);

    Optional<Course> findById(UUID id);

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    List<Course> findAll();
}
