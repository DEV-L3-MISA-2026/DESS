package com.dess.assessment.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultCorrection;

/**
 * @brief Application API of the result entry and retrieval.
 *
 * <p>One student has at most one result per assessment. A result is
 * recorded once, then corrected when a mistake is found. A correction
 * keeps who entered the result and when it was entered, and only
 * moves the update time. Every correction is also kept in the audit
 * trail, with the previous value, the new value, who corrected it
 * and when, so the original value stays readable.</p>
 */
public interface ResultService
{
    /**
     * @brief One result with its correction trail, for the detail view.
     *
     * @param result the result as stored now
     * @param corrections the correction trail, oldest first; empty
     *                    when the result was never corrected
     */
    record ResultDetail(Result result, List<ResultCorrection> corrections)
    {
    }

    Result recordResult(RecordResultCommand command);

    Result correctResult(UUID resultId, CorrectResultCommand command);

    Optional<Result> findById(UUID id);

    Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId);

    List<Result> listByAssessment(UUID assessmentId);

    List<Result> listByStudent(long studentPersonId);

    /**
     * @brief Returns the correction trail of one result.
     *
     * @param resultId the corrected result
     * @return the entries in correction order, oldest first
     */
    List<ResultCorrection> correctionsFor(UUID resultId);

    /**
     * @brief Returns one result with its correction trail.
     *
     * @param id the result
     * @return the result and its trail
     */
    ResultDetail detail(UUID id);
}
