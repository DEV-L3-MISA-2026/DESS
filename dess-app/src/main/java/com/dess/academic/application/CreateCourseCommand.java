package com.dess.academic.application;

/** Données nécessaires à la création d'un cours. */
public record CreateCourseCommand(String code, String title) {
    public CreateCourseCommand {
        if (code == null || code.trim().isBlank()) {
            throw new IllegalArgumentException("Le code du cours est obligatoire");
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Le titre du cours est obligatoire");
        }
    }
}
