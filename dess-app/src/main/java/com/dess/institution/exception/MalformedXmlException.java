package com.dess.institution.exception;

/** Le fichier n'est pas du XML bien formé (balises croisées, DOCTYPE interdit, etc.). */
public class MalformedXmlException extends ConfigurationLoadException {

    private static final long serialVersionUID = 1L;

    public MalformedXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
