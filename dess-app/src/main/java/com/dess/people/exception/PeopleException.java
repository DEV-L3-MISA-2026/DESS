package com.dess.people.exception;

/** Exception de base du module people. */
public abstract class PeopleException extends RuntimeException {
    protected PeopleException(String message) {
        super(message);
    }

    protected PeopleException(String message, Throwable cause) {
        super(message, cause);
    }
}
