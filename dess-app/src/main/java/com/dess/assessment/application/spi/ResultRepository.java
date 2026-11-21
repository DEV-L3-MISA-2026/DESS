package com.dess.assessment.application.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.Result;

/** @brief Persistence port for the results. */
public interface ResultRepository
{
    Result save(Result result);

    Optional<Result> findById(UUID id);

    Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId);

    List<Result> findByAssessment(UUID assessmentId);

    List<Result> findByStudent(long studentPersonId);

    boolean existsByAssessmentAndStudent(UUID assessmentId, long studentPersonId);
}
