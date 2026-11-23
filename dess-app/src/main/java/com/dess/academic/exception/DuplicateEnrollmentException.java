package com.dess.academic.exception;

import java.util.UUID;

/** Levée lorsqu'une inscription identique existe déjà. */
public class DuplicateEnrollmentException extends AcademicException
{
    public DuplicateEnrollmentException(UUID studentId, UUID groupId, UUID periodId)
    {
        super("Student " + studentId + " is already enrolled in group " + groupId
                + " for period " + periodId);
    }

    public DuplicateEnrollmentException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
