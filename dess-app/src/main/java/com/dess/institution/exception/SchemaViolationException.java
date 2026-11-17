package com.dess.institution.exception;

import java.util.ArrayList;
import java.util.List;

/** Le XML est bien formé mais ne respecte pas {@code institution.xsd}. */
public class SchemaViolationException extends ConfigurationLoadException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<String> violations;

    public SchemaViolationException(String source, List<String> violations) {
        super("Le fichier " + source + " ne respecte pas le schéma XSD : " + String.join(" ; ", violations));
        this.violations = new ArrayList<>(violations);
    }

    public List<String> getViolations() {
        return List.copyOf(violations);
    }
}
