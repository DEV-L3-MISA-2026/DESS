package com.dess.assessment.exception;

/** @brief Thrown when an assessment is not found. */
public class AssessmentNotFoundException extends AssessmentException
{
    public AssessmentNotFoundException(String message)
    {
        super(message);
    }
}
