package com.dess.institution.application;

import java.util.List;

/**
 * Résultat d'une vérification sans effet de bord.
 *
 * @param errors violations détectées, vide si tout est valide
 */
public record ValidationReport(List<String> errors) {

    public ValidationReport {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
