package com.dess.people.application;

import com.dess.people.application.validation.StudentInputValidator;

/** Données nécessaires à la création d’un étudiant. */
public record CreateStudentCommand(String email, String lastName, String firstName, String registrationNumber) {
    public CreateStudentCommand {
        StudentInputValidator.validate(email, lastName, firstName);
        if (registrationNumber == null || registrationNumber.trim().isBlank()) {
            throw new IllegalArgumentException("Le numéro d’inscription est obligatoire");
        }
    }
}
