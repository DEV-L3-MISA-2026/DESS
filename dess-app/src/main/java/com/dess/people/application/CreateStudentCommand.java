package com.dess.people.application;

/** Données nécessaires à la création d’un étudiant. */
public record CreateStudentCommand(String email, String lastName, String firstName, String registrationNumber) {
    public CreateStudentCommand {
        if (email == null || email.trim().isBlank()) throw new IllegalArgumentException("L’adresse e-mail est obligatoire");
        if (lastName == null || lastName.trim().isBlank()) throw new IllegalArgumentException("Le nom est obligatoire");
        if (firstName == null || firstName.trim().isBlank()) throw new IllegalArgumentException("Le prénom est obligatoire");
        if (registrationNumber == null || registrationNumber.trim().isBlank()) throw new IllegalArgumentException("Le numéro d’inscription est obligatoire");
    }
}
