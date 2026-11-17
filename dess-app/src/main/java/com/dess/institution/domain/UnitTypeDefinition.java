package com.dess.institution.domain;

import java.util.List;
import java.util.Optional;

/**
 * Type d'unité défini par l'utilisateur (ex. « MENTION »). À ne pas confondre avec
 * {@link OrganizationalUnit}, qui est une unité concrète (ex. « Mention Informatique »).
 *
 * @param code        code unique du type
 * @param label       libellé
 * @param description description facultative
 * @param active      statut actif/inactif (un type inactif n'accepte plus de nouvelles unités)
 * @param attributes  définitions d'attributs personnalisés
 */
public record UnitTypeDefinition(
        String code,
        String label,
        String description,
        boolean active,
        List<AttributeDefinition> attributes) {

    public UnitTypeDefinition {
        code = Texts.orEmpty(code);
        label = Texts.orEmpty(label);
        description = Texts.blankToNull(description);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    public static UnitTypeDefinition of(String code, String label) {
        return new UnitTypeDefinition(code, label, null, true, List.of());
    }

    public static UnitTypeDefinition of(String code, String label, List<AttributeDefinition> attributes) {
        return new UnitTypeDefinition(code, label, null, true, attributes);
    }

    public Optional<AttributeDefinition> findAttribute(String key) {
        return attributes.stream().filter(a -> a.key().equals(key)).findFirst();
    }

    public UnitTypeDefinition withLabelAndDescription(String newLabel, String newDescription) {
        return new UnitTypeDefinition(code, newLabel, newDescription, active, attributes);
    }

    public UnitTypeDefinition withActive(boolean newActive) {
        return new UnitTypeDefinition(code, label, description, newActive, attributes);
    }

    public UnitTypeDefinition withAttributes(List<AttributeDefinition> newAttributes) {
        return new UnitTypeDefinition(code, label, description, active, newAttributes);
    }
}
