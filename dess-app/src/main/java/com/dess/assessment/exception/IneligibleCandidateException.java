package com.dess.assessment.exception;

/** @brief Thrown when a student is not eligible for an assessment. */
public class IneligibleCandidateException extends AssessmentException
{
    public IneligibleCandidateException(String message)
    {
        super(message);
    }
}
