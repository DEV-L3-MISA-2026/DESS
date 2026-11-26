package com.dess.assessment.application.spi;

import java.util.List;
import java.util.UUID;

import com.dess.assessment.domain.ResultCorrection;

/** @brief Persistence port for the result correction history. */
public interface ResultCorrectionRepository
{
    ResultCorrection save(ResultCorrection correction);

    /**
     * @brief Returns the correction trail of one result.
     *
     * @param resultId the corrected result
     * @return the entries in correction order, oldest first
     */
    List<ResultCorrection> findByResultId(UUID resultId);
}
