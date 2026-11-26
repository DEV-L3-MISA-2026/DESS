package com.dess.people.application;

import com.dess.people.application.validation.StudentInputValidator;

/** Données nécessaires à la modification d’un étudiant. */
public record UpdateStudentCommand(String email, String lastName, String firstName) {
    public UpdateStudentCommand {
        StudentInputValidator.validate(email, lastName, firstName);
    }
}
