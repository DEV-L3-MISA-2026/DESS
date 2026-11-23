package com.dess.people.application;

/** Données nécessaires à la création d’un enseignant. */
public record CreateTeacherCommand(String email, String lastName, String firstName, String academicRank) {
    public CreateTeacherCommand {
        if (email == null || email.trim().isBlank()) throw new IllegalArgumentException("L’adresse e-mail est obligatoire");
        if (lastName == null || lastName.trim().isBlank()) throw new IllegalArgumentException("Le nom est obligatoire");
        if (firstName == null || firstName.trim().isBlank()) throw new IllegalArgumentException("Le prénom est obligatoire");
        if (academicRank == null || academicRank.trim().isBlank()) throw new IllegalArgumentException("Le grade académique est obligatoire");
    }
}
