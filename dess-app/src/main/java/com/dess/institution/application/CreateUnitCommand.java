package com.dess.institution.application;

import java.util.Map;

/**
 * Données nécessaires à la création d'une unité.
 *
 * @param id          identifiant souhaité ; si {@code null} ou vide, un UUID est généré
 * @param code        code métier, unique dans l'établissement
 * @param name        nom
 * @param description description facultative
 * @param typeCode    code du type d'unité (doit exister et être actif)
 * @param parentId    identifiant du parent, {@code null} pour créer une racine
 * @param active      statut initial
 * @param values      valeurs des attributs personnalisés (clé → valeur sérialisée) ; peut être {@code null}
 */
public record CreateUnitCommand(
        String id,
        String code,
        String name,
        String description,
        String typeCode,
        String parentId,
        boolean active,
        Map<String, String> values) {

    /** Crée une unité active, sans description ni identifiant imposé. */
    public static CreateUnitCommand of(String code, String name, String typeCode, String parentId,
                                       Map<String, String> values) {
        return new CreateUnitCommand(null, code, name, null, typeCode, parentId, true, values);
    }
}
