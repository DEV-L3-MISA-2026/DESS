package com.dess.people.domain;

import java.util.Objects;

/** Profil immuable d’un étudiant. */
public record StudentProfile(long personId, String registrationNumber) {
    public StudentProfile {
        if (personId <= 0) throw new IllegalArgumentException("L’identifiant doit être positif");
        Objects.requireNonNull(registrationNumber, "Le numéro d’inscription est obligatoire");
        registrationNumber = registrationNumber.trim();
        if (registrationNumber.isBlank()) throw new IllegalArgumentException("Le numéro d’inscription est obligatoire");
    }
}
