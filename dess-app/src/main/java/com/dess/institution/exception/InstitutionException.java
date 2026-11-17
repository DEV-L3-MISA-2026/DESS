package com.dess.institution.exception;

/** Racine de toutes les exceptions (non contrôlées) du module institution. */
public class InstitutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InstitutionException(String message) {
        super(message);
    }

    public InstitutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
