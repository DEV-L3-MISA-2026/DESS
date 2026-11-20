package com.dess.academic.application;

import com.dess.academic.domain.Course;
import java.util.List;
import java.util.UUID;

/** API applicative du catalogue des cours. */
public interface CourseService {
    Course create(CreateCourseCommand command);

    Course get(UUID id);

    List<Course> list();
}
