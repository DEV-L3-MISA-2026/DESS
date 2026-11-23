package com.dess.academic.exception;

/** Levée lorsqu'un cours est introuvable. */
public class CourseNotFoundException extends AcademicException {
    public CourseNotFoundException(String message) {
        super(message);
    }
}
