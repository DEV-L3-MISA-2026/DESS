package com.dess.institution.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Levée lorsqu'une opération violerait une règle métier (relation parent-enfant interdite, code
 * dupliqué, valeur invalide, attribut obligatoire manquant...). La configuration reste inchangée.
 */
public class ValidationException extends InstitutionException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<String> violations;

    public ValidationException(String violation) {
        this(List.of(violation));
    }

    public ValidationException(List<String> violations) {
        super(String.join(" ; ", violations));
        this.violations = new ArrayList<>(violations);
    }

    /** Liste de toutes les violations détectées, lisibles par un humain. */
    public List<String> getViolations() {
        return List.copyOf(violations);
    }
}
