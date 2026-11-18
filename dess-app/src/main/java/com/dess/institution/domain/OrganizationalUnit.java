package com.dess.institution.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @brief Concrete organizational unit in the institution tree.
 *
 * <p>The {@code id} is stable and never changes, even when the unit is moved. The {@code code} is
 * the business code, and {@code typeCode} references a {@link UnitTypeDefinition}. Children are not
 * stored in the object; they are retrieved through
 * {@link com.dess.institution.application.HierarchyService#getChildren(String)}.</p>
 *
 * @param id          stable, unique technical identifier
 * @param code        business code, unique within the institution
 * @param name        display name
 * @param description optional description
 * @param typeCode    code of the unit type
 * @param parentId    identifier of the parent unit, or {@code null} for a root unit
 * @param active      activation state
 * @param values      read-only custom attribute values keyed by attribute key
 */
public record OrganizationalUnit(
    String id,
    String code,
    String name,
    String description,
    String typeCode,
    String parentId,
    boolean active,
    Map<String, AttributeValue> values)
{
    /**
     * Normalizes text fields and defensively copies the attribute values.
     *
     * @param id          stable technical identifier
     * @param code        business code
     * @param name        display name
     * @param description optional description
     * @param typeCode    unit type code
     * @param parentId    parent identifier, or {@code null} for a root unit
     * @param active      activation state
     * @param values      custom attribute values; {@code null} is treated as an empty map
     */
    public OrganizationalUnit
    {
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

    /**
     * Indicates whether the unit is a root.
     *
     * @return {@code true} when the unit has no parent
     */
    public boolean isRoot()
    {
        return parentId == null;
    }

    /**
     * Returns one attribute value.
     *
     * @param key attribute key
     * @return the value, or an empty optional when the unit does not define it
     */
    public Optional<AttributeValue> value(String key)
    {
        return Optional.ofNullable(values.get(key));
    }

    /**
     * Copies this unit with a different parent.
     *
     * @param newParentId new parent identifier, or {@code null} to make the unit a root
     * @return a new unit with the supplied parent
     */
    public OrganizationalUnit withParentId(String newParentId)
    {
        return new OrganizationalUnit(id, code, name, description, typeCode, newParentId, active, values);
    }

    /**
     * Copies this unit with a different activation state.
     *
     * @param newActive new activation state
     * @return a new unit with the supplied state
     */
    public OrganizationalUnit withActive(boolean newActive)
    {
        return new OrganizationalUnit(id, code, name, description, typeCode, parentId, newActive, values);
    }

    /**
     * Copies this unit with a different set of attribute values.
     *
     * @param newValues replacement attribute values
     * @return a new unit with the supplied values
     */
    public OrganizationalUnit withValues(Map<String, AttributeValue> newValues)
    {
        return new OrganizationalUnit(id, code, name, description, typeCode, parentId, active, newValues);
    }

    /**
     * Copies this unit with updated editable details.
     *
     * @param newCode        new business code
     * @param newName        new display name
     * @param newDescription new optional description
     * @return a new unit with the supplied details
     */
    public OrganizationalUnit withDetails(String newCode, String newName, String newDescription)
    {
        return new OrganizationalUnit(id, newCode, newName, newDescription, typeCode, parentId, active, values);
    }
}
