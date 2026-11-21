package com.dess.assessment.exception;

/** @brief Thrown when a result cannot be recorded as asked. */
public class InvalidResultException extends AssessmentException
{
    public InvalidResultException(String message)
    {
        super(message);
    }
}
