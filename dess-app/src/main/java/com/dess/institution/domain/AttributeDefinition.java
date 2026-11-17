package com.dess.institution.domain;

import java.util.Objects;

/**
 * Définition d'un attribut personnalisé : décrit un champ disponible pour toutes les unités
 * d'un type donné. La valeur réelle d'une unité est un {@link AttributeValue}.
 *
 * @param key          clé unique dans son type (ex. {@code chefMention})
 * @param label        libellé affichable
 * @param description  description facultative
 * @param type         type de donnée, utilisé pour valider les valeurs
 * @param required     {@code true} si toute unité du type doit renseigner une valeur
 * @param defaultValue valeur par défaut facultative (sérialisée), appliquée à la création d'une unité
 */
public record AttributeDefinition(
        String key,
        String label,
        String description,
        AttributeType type,
        boolean required,
        String defaultValue) {

    public AttributeDefinition {
        key = Texts.orEmpty(key);
        label = Texts.orEmpty(label);
        description = Texts.blankToNull(description);
        type = Objects.requireNonNull(type, "type");
        defaultValue = Texts.blankToNull(defaultValue);
    }

    public static AttributeDefinition of(String key, String label, AttributeType type, boolean required) {
        return new AttributeDefinition(key, label, null, type, required, null);
    }

    public static AttributeDefinition of(String key, String label, AttributeType type, boolean required,
                                         String defaultValue) {
        return new AttributeDefinition(key, label, null, type, required, defaultValue);
    }

    public boolean hasDefault() {
        return defaultValue != null;
    }
}
