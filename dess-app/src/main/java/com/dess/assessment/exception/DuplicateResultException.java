package com.dess.assessment.exception;

/** @brief Thrown when a result already exists for an assessment and a student. */
public class DuplicateResultException extends AssessmentException
{
    public DuplicateResultException(String message)
    {
        super(message);
    }
}
