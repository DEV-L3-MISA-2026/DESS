package com.dess.academic.exception;

import java.util.UUID;

/** Levée lorsqu'une inscription est introuvable. */
public class EnrollmentNotFoundException extends AcademicException
{
    public EnrollmentNotFoundException(UUID enrollmentId)
    {
        super("Enrollment introuvable : " + enrollmentId);
    }
}
