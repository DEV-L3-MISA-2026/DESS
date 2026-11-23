package com.dess.academic.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Cours immuable du catalogue académique. */
public record Course(UUID id, String code, String title, boolean active, Instant createdAt) {
    public Course {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        code = normalizeCode(code);
        title = requireText(title, "Le titre du cours est obligatoire");
    }

    /** Crée un cours actif avec un identifiant et une date de création neufs. */
    public static Course create(String code, String title) {
        return new Course(UUID.randomUUID(), code, title, true, Instant.now());
    }

    /** Renomme le cours sans modifier son identité ni son état. */
    public Course renamed(String newTitle) {
        return new Course(id, code, newTitle, active, createdAt);
    }

    /** Désactive le cours sans le supprimer du catalogue. */
    public Course deactivated() {
        return new Course(id, code, title, false, createdAt);
    }

    /** Normalise le code : texte obligatoire, sans espaces superflus, en majuscules. */
    public static String normalizeCode(String code) {
        return requireText(code, "Le code du cours est obligatoire").toUpperCase();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
