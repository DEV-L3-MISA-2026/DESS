package com.dess.academic.exception;

/** Levée lorsqu'une inscription ne respecte pas les règles métier. */
public class InvalidEnrollmentException extends AcademicException {
    public InvalidEnrollmentException(String message) { super(message); }
}
