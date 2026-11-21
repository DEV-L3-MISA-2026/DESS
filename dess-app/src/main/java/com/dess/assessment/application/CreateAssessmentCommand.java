package com.dess.assessment.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.dess.assessment.domain.AssessmentType;

/** @brief Data needed to create an assessment. */
public record CreateAssessmentCommand(UUID courseId, String title, AssessmentType type,
                                      BigDecimal maximumScore)
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
    }
}
