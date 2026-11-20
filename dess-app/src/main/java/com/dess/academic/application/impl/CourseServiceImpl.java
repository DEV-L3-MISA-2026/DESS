package com.dess.academic.application.impl;

import com.dess.academic.application.CourseService;
import com.dess.academic.application.CreateCourseCommand;
import com.dess.academic.domain.Course;
import com.dess.academic.exception.CourseNotFoundException;
import com.dess.academic.exception.DuplicateCourseCodeException;
import com.dess.academic.infrastructure.CourseRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

/** Implémentation applicative du catalogue des cours. */
@Transactional
public class CourseServiceImpl implements CourseService {
    private final CourseRepository repository;

    public CourseServiceImpl(CourseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Course create(CreateCourseCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        String code = Course.normalizeCode(command.code());
        if (repository.existsByCode(code)) throw new DuplicateCourseCodeException(code);
        return repository.save(Course.create(code, command.title()));
    }

    @Override
    public Course get(UUID id) {
        if (id == null) throw new IllegalArgumentException("L'identifiant du cours est obligatoire");
        return repository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Cours introuvable : " + id));
    }

    @Override
    public List<Course> list() {
        return repository.findAll();
    }
}
