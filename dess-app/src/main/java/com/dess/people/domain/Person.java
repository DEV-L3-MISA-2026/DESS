package com.dess.people.domain;

import java.util.Objects;

/** Personne immuable commune aux profils étudiant et enseignant. */
public record Person(Long id, String email, String lastName, String firstName, boolean active) {
    public Person {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(firstName, "firstName");
        if (email.isBlank()) throw new IllegalArgumentException("L'adresse e-mail est obligatoire");
        if (lastName.isBlank()) throw new IllegalArgumentException("Le nom est obligatoire");
        if (firstName.isBlank()) throw new IllegalArgumentException("Le prénom est obligatoire");
    }
}
