package com.dess.assessment.domain;

/**
 * @brief An exam session cannot be booked because a resource is already reserved.
 *
 * <p>The message names the conflicting resource so the web layer can explain the
 * conflict in text instead of only showing a generic failure state.</p>
 */
public class ExamSessionConflictException extends RuntimeException
{
    public ExamSessionConflictException(String message)
    {
        super(message);
    }
}
