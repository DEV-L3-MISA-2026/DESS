package com.dess.academic.exception;

/** Exception de base du module academic. */
public class AcademicException extends RuntimeException {
    public AcademicException(String message) { super(message); }
    public AcademicException(String message, Throwable cause) { super(message, cause); }
}
