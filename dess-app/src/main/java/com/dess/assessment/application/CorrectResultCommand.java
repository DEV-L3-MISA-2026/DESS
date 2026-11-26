package com.dess.assessment.application;

import java.math.BigDecimal;

import com.dess.assessment.domain.ResultStatus;

/**
 * @brief Data needed to correct a result already entered.
 *
 * <p>The corrector is required: every correction is kept in the
 * audit trail with who made it, so a correction without a named
 * corrector cannot be recorded.</p>
 */
public record CorrectResultCommand(BigDecimal score, ResultStatus status, String correctedBy)
{
    public CorrectResultCommand
    {
        if (status == null)
        {
            throw new IllegalArgumentException("status is required");
        }
        if (correctedBy == null || correctedBy.isBlank())
        {
            throw new IllegalArgumentException("correctedBy is required");
        }
    }
}
