package com.dess.assessment.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.dess.assessment.domain.AssessmentType;

/**
 * @brief Data needed to create an assessment.
 *
 * <p>The duration is in minutes. It is boxed on purpose: a missing
 * duration (null) is rejected here with a readable message, while a
 * zero or negative duration is rejected by the assessment domain
 * record. Both failures are {@link IllegalArgumentException}, which
 * the API error mapper turns into HTTP 400, never 500.</p>
 */
public record CreateAssessmentCommand(UUID courseId, String title, AssessmentType type,
                                      BigDecimal maximumScore, Integer durationMinutes)
{
    public CreateAssessmentCommand
    {
        if (courseId == null)
        {
            throw new IllegalArgumentException("courseId is required");
        }
        if (title == null || title.isBlank())
        {
            throw new IllegalArgumentException("title is required");
        }
        if (type == null)
        {
            throw new IllegalArgumentException("type is required");
        }
        if (maximumScore == null)
        {
            throw new IllegalArgumentException("maximumScore is required");
        }
        if (durationMinutes == null)
        {
            throw new IllegalArgumentException("durationMinutes is required");
        }
    }
}
