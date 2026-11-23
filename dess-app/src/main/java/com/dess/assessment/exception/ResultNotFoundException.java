package com.dess.assessment.exception;

/** @brief Thrown when a result is not found. */
public class ResultNotFoundException extends AssessmentException
{
    public ResultNotFoundException(String message)
    {
        super(message);
    }
}
