package com.dess.institution.exception;

/** Échec d'écriture du fichier XML. L'ancien fichier et l'état en mémoire restent inchangés. */
public class ConfigurationSaveException extends InstitutionException {

    private static final long serialVersionUID = 1L;

    public ConfigurationSaveException(String message) {
        super(message);
    }

    public ConfigurationSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
