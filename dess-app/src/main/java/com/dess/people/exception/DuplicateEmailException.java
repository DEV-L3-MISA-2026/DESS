package com.dess.people.exception;

/** Levée lorsqu’une adresse e-mail est déjà utilisée. */
public class DuplicateEmailException extends DuplicatePersonException {
    public DuplicateEmailException(String email) {
        super("Cette adresse e-mail existe déjà : " + email);
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super("Cette adresse e-mail existe déjà : " + email, cause);
    }
}
