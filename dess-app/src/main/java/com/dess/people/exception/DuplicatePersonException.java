package com.dess.people.exception;

/** Levée lorsqu'un e-mail ou un matricule existe déjà. */
public class DuplicatePersonException extends PeopleException {
    public DuplicatePersonException(String message) { super(message); }
    public DuplicatePersonException(String message, Throwable cause) { super(message, cause); }
}
