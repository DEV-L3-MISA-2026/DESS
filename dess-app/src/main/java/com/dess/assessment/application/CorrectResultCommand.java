package com.dess.assessment.application;

import java.math.BigDecimal;

import com.dess.assessment.domain.ResultStatus;

/** @brief Data needed to correct a result already entered. */
public record CorrectResultCommand(BigDecimal score, ResultStatus status)
{
    public CorrectResultCommand
    {
        if (status == null)
        {
            throw new IllegalArgumentException("status is required");
        }
    }
}
