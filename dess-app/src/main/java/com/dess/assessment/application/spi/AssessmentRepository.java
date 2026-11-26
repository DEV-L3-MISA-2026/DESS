package com.dess.assessment.application.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.Assessment;

/** @brief Persistence port for the assessment definitions. */
public interface AssessmentRepository
{
    Assessment save(Assessment assessment);

    Optional<Assessment> findById(UUID id);

    List<Assessment> findAll();

    List<Assessment> findByCourseId(UUID courseId);
}
