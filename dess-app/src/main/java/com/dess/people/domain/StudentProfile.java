package com.dess.people.domain;

import java.util.Objects;

/** Profil immuable d'un étudiant. */
public record StudentProfile(long personId, String registrationNumber) {
    public StudentProfile {
        Objects.requireNonNull(registrationNumber, "registrationNumber");
        if (registrationNumber.isBlank()) throw new IllegalArgumentException("Le numéro d'inscription est obligatoire");
    }
}
