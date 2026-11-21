package com.dess.academic.application;

/** Données de création d'un programme académique. */
public record CreateProgramCommand(String code, String name) {
    public CreateProgramCommand {
        if (code == null || code.trim().isBlank()) {
            throw new IllegalArgumentException("Le code du programme est obligatoire");
        }
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Le nom du programme est obligatoire");
        }
    }
}
