package com.dess.academic.exception;

/** Levée lorsqu'un code de cours est déjà utilisé. */
public class DuplicateCourseCodeException extends AcademicException {
    public DuplicateCourseCodeException(String code) {
        super("Un cours avec le code '" + code + "' existe déjà");
    }
}
