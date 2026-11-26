package com.dess.people.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/** Regroupe toutes les violations de validation d’un étudiant. */
public class StudentValidationException extends IllegalArgumentException {
    private final Map<String, String> fieldErrors;

    public StudentValidationException(Map<String, String> fieldErrors) {
        super(buildMessage(fieldErrors));
        this.fieldErrors = Map.copyOf(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    private static String buildMessage(Map<String, String> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) return "Données étudiant invalides";
        return String.join("; ", fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .toList());
    }
}
