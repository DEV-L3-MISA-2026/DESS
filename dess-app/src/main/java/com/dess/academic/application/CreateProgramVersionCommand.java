package com.dess.academic.application;

import java.time.LocalDate;

/** Données de création d'une version de programme. */
public record CreateProgramVersionCommand(long programId, String code, LocalDate effectiveFrom,
                                         LocalDate effectiveTo) {
    public CreateProgramVersionCommand {
        if (programId <= 0) throw new IllegalArgumentException("L'identifiant du programme doit être positif");
        if (code == null || code.trim().isBlank()) {
            throw new IllegalArgumentException("Le code de la version est obligatoire");
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("La date d'entrée en vigueur est obligatoire");
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("La date de fin ne peut précéder la date de début");
        }
    }
}
