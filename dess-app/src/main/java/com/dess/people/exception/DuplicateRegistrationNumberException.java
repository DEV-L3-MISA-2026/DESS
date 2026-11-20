package com.dess.people.exception;

/** Levée lorsqu’un numéro d’inscription existe déjà. */
public class DuplicateRegistrationNumberException extends DuplicatePersonException {
    public DuplicateRegistrationNumberException(String registrationNumber) {
        super("Ce matricule existe déjà : " + registrationNumber);
    }
}
