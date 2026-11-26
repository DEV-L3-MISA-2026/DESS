package com.dess.academic.exception;

/** Levée lorsqu'une période académique existe déjà. */
public class DuplicatePeriodException extends AcademicException {
    public DuplicatePeriodException(String message) { super(message); }
    public DuplicatePeriodException(String message, Throwable cause) { super(message, cause); }
}
