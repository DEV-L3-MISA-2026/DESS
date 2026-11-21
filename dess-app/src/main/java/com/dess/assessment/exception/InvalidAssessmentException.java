package com.dess.assessment.exception;

/** @brief Thrown when an assessment cannot be created or used as asked. */
public class InvalidAssessmentException extends AssessmentException
{
    public InvalidAssessmentException(String message)
    {
        super(message);
    }

    public InvalidAssessmentException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
