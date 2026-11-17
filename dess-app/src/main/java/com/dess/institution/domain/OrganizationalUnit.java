package com.dess.institution.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Unité organisationnelle concrète de l'arbre (ex. « Mention Informatique »).
 * <p>
 * L'{@code id} est stable : il ne change jamais, même si l'unité est déplacée. Le {@code code} est
 * le code métier, et {@code typeCode} le code du {@link UnitTypeDefinition}. Les enfants ne sont pas
 * stockés dans l'objet : ils sont obtenus via {@code HierarchyService#getChildren(String)}.
 *
 * @param id          identifiant technique stable et unique
 * @param code        code métier, unique dans l'établissement
 * @param name        nom
 * @param description description facultative
 * @param typeCode    code du type d'unité
 * @param parentId    identifiant du parent, {@code null} pour une unité racine
 * @param active      statut actif/inactif
 * @param values      valeurs des attributs personnalisés, indexées par clé (lecture seule)
 */
public record OrganizationalUnit(
        String id,
        String code,
        String name,
        String description,
        String typeCode,
        String parentId,
        boolean active,
        Map<String, AttributeValue> values) {

    public OrganizationalUnit {
        id = Texts.orEmpty(id);
        code = Texts.orEmpty(code);
        name = Texts.orEmpty(name);
        description = Texts.blankToNull(description);
        typeCode = Texts.orEmpty(typeCode);
        parentId = Texts.blankToNull(parentId);
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public Optional<AttributeValue> value(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public OrganizationalUnit withParentId(String newParentId) {
        return new OrganizationalUnit(id, code, name, description, typeCode, newParentId, active, values);
    }

    public OrganizationalUnit withActive(boolean newActive) {
        return new OrganizationalUnit(id, code, name, description, typeCode, parentId, newActive, values);
    }

    public OrganizationalUnit withValues(Map<String, AttributeValue> newValues) {
        return new OrganizationalUnit(id, code, name, description, typeCode, parentId, active, newValues);
    }

    public OrganizationalUnit withDetails(String newCode, String newName, String newDescription) {
        return new OrganizationalUnit(id, newCode, newName, newDescription, typeCode, parentId, active, values);
    }
}
