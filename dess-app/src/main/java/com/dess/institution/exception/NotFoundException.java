package com.dess.institution.exception;

/** Levée lorsqu'une unité, un type ou un attribut demandé n'existe pas. */
public class NotFoundException extends InstitutionException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
