package com.dess.academic.exception;

/** Levée lorsqu'une inscription entre en conflit avec une règle unique. */
public class EnrollmentConflictException extends AcademicException {
    public EnrollmentConflictException(String message) {
        super(message);
    }

    public EnrollmentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
