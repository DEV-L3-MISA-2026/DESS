package com.dess.institution.application;

import com.dess.institution.domain.InstitutionConfig;

/**
 * Informations générales et état de la configuration de l'établissement.
 * <p>
 * API publique du module : les autres modules et la couche web (servlets / beans CDI) n'utilisent
 * que ces interfaces, jamais le format XML ni les classes d'infrastructure.
 */
public interface InstitutionService {

    /** @return {@link ConfigurationStatus#READY} si une configuration est chargée */
    ConfigurationStatus getStatus();

    /**
     * Crée explicitement l'établissement (sans types ni unités) lorsqu'aucune configuration n'existe.
     *
     * @throws com.dess.institution.exception.ValidationException si déjà initialisé ou données invalides
     */
    InstitutionInfo initialize(String code, String name, String description);

    /**
     * @return la configuration complète (instantané immuable)
     * @throws com.dess.institution.exception.NotInitializedException si aucune configuration n'est chargée
     */
    InstitutionConfig getConfig();

    InstitutionInfo getInfo();

    /** Modifie le code, le nom et la description ; l'identifiant ne change jamais. */
    InstitutionInfo updateInfo(String code, String name, String description);

    /** Revalide toute la configuration en mémoire sans rien modifier. */
    ValidationReport check();

    /** Recharge la configuration depuis le fichier XML (ex. après édition manuelle). */
    void reload();
}
