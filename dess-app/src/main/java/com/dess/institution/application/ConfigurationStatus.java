package com.dess.institution.application;

/** État de la configuration de l'établissement. */
public enum ConfigurationStatus {
    /** Aucune configuration chargée : un fichier XML initial (ou une initialisation explicite) est nécessaire. */
    NOT_INITIALIZED,
    /** Configuration chargée et valide. */
    READY
}
