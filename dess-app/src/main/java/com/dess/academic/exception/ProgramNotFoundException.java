package com.dess.academic.exception;

/** Levée lorsqu'un programme académique est introuvable. */
public class ProgramNotFoundException extends AcademicException {
    public ProgramNotFoundException(String message) { super(message); }
}
