package com.dess.academic.exception;

/** Levée lorsqu'une version de programme est introuvable. */
public class ProgramVersionNotFoundException extends AcademicException {
    public ProgramVersionNotFoundException(String message) { super(message); }
}
