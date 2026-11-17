package com.dess.institution.domain;

/**
 * Valeur réelle d'un attribut pour une unité précise. La valeur est conservée sous forme sérialisée
 * (texte) ; sa conversion et sa validation sont centralisées dans
 * {@code com.dess.institution.validation.AttributeValueConverter}.
 *
 * @param key   clé de la {@link AttributeDefinition} correspondante
 * @param value valeur sérialisée
 */
public record AttributeValue(String key, String value) {

    public AttributeValue {
        key = Texts.orEmpty(key);
        value = Texts.orEmpty(value);
    }
}
