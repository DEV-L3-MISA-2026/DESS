package com.dess.assessment.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.Result;

/**
 * @brief Application API of the result entry and retrieval.
 *
 * <p>One student has at most one result per assessment. A result is
 * recorded once, then corrected when a mistake is found. A correction
 * keeps who entered the result and when it was entered, and only
 * moves the update time.</p>
 */
public interface ResultService
{
    Result recordResult(RecordResultCommand command);

    Result correctResult(UUID resultId, CorrectResultCommand command);

    Optional<Result> findById(UUID id);

    Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId);

    List<Result> listByAssessment(UUID assessmentId);

    List<Result> listByStudent(long studentPersonId);
}
