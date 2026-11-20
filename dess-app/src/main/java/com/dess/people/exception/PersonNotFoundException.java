package com.dess.people.exception;

/** Levée lorsqu’une personne demandée n’existe pas. */
public class PersonNotFoundException extends PeopleException {
    public PersonNotFoundException(String message) {
        super(message);
    }
}
