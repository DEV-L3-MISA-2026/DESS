package com.dess.assessment.exception;

/**
 * @brief Base exception of the assessment module.
 */
public class AssessmentException extends RuntimeException
{
    public AssessmentException(String message)
    {
        super(message);
    }

    public AssessmentException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
