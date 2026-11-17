package com.dess.institution.exception;

/** Échec de lecture du fichier XML de configuration (E/S, XML mal formé, non conforme au XSD). */
public class ConfigurationLoadException extends InstitutionException {

    private static final long serialVersionUID = 1L;

    public ConfigurationLoadException(String message) {
        super(message);
    }

    public ConfigurationLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
