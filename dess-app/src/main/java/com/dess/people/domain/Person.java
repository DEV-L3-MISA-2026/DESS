package com.dess.people.domain;

import java.util.Objects;

/** Personne immuable commune aux profils étudiant et enseignant. */
public record Person(long id, String email, String lastName, String firstName, boolean active) {
    public Person {
        if (id <= 0) throw new IllegalArgumentException("L’identifiant doit être positif");
        email = requireTrimmedText(email, "L’adresse e-mail est obligatoire");
        lastName = requireTrimmedText(lastName, "Le nom est obligatoire");
        firstName = requireTrimmedText(firstName, "Le prénom est obligatoire");
    }

    private static String requireTrimmedText(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.trim().isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
