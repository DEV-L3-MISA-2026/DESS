package com.dess.institution.exception;

/**
 * Levée lorsqu'aucune configuration d'établissement n'existe encore (fichier XML absent).
 * Une configuration initiale doit être fournie explicitement (fichier XML ou
 * {@code InstitutionService#initialize}) : le module n'invente jamais d'établissement.
 */
public class NotInitializedException extends InstitutionException {

    private static final long serialVersionUID = 1L;

    public NotInitializedException() {
        super("Aucune configuration d'établissement n'est chargée : fournir le fichier XML initial "
                + "ou initialiser l'établissement.");
    }
}
