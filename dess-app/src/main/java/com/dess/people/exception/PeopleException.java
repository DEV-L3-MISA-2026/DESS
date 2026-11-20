package com.dess.people.exception;

/** Exception de base du module people. */
public class PeopleException extends RuntimeException {
    public PeopleException(String message) { super(message); }
    public PeopleException(String message, Throwable cause) { super(message, cause); }
}
