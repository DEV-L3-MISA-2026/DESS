package com.dess.assessment.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.dess.assessment.domain.ResultStatus;

/** @brief Data needed to record a result. */
public record RecordResultCommand(UUID assessmentId, UUID examSessionId, long studentPersonId,
                                  BigDecimal score, ResultStatus status, String enteredBy)
{
    public RecordResultCommand
    {
        if (assessmentId == null)
        {
            throw new IllegalArgumentException("assessmentId is required");
        }
        if (studentPersonId <= 0)
        {
            throw new IllegalArgumentException("studentPersonId must be greater than zero");
        }
        if (status == null)
        {
            throw new IllegalArgumentException("status is required");
        }
        if (enteredBy == null || enteredBy.isBlank())
        {
            throw new IllegalArgumentException("enteredBy is required");
        }
    }
}
