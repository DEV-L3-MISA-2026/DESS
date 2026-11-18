package com.dess.institution.domain;

import java.util.List;
import java.util.Optional;

/**
 * @brief User-defined type of organizational unit, such as {@code DEPARTMENT}.
 *
 * <p>Not to be confused with {@link OrganizationalUnit}, which is a concrete unit such as
 * "Computer Science Department".</p>
 *
 * @param code        unique type code
 * @param label       human-readable label
 * @param description optional description
 * @param active      activation state; inactive types no longer accept new units
 * @param attributes  custom attribute definitions
 */
public record UnitTypeDefinition(
    String code,
    String label,
    String description,
    boolean active,
    List<AttributeDefinition> attributes)
{
    /**
     * Normalizes text fields and defensively copies the attribute list.
     *
     * @param code        unique type code
     * @param label       human-readable label
     * @param description optional description
     * @param active      activation state
     * @param attributes  attribute definitions; {@code null} is treated as an empty list
     */
    public UnitTypeDefinition
    {
        code = Texts.orEmpty(code);
        label = Texts.orEmpty(label);
        description = Texts.blankToNull(description);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }

    /**
     * Creates an active type without description or attributes.
     *
     * @param code  unique type code
     * @param label human-readable label
     * @return the new unit type
     */
    public static UnitTypeDefinition of(String code, String label)
    {
        return new UnitTypeDefinition(code, label, null, true, List.of());
    }

    /**
     * Creates an active type with attribute definitions and no description.
     *
     * @param code       unique type code
     * @param label      human-readable label
     * @param attributes attribute definitions
     * @return the new unit type
     */
    public static UnitTypeDefinition of(String code, String label, List<AttributeDefinition> attributes)
    {
        return new UnitTypeDefinition(code, label, null, true, attributes);
    }

    /**
     * Finds an attribute definition by key.
     *
     * @param key attribute key
     * @return the definition, or an empty optional when it does not exist
     */
    public Optional<AttributeDefinition> findAttribute(String key)
    {
        return attributes.stream().filter(attribute -> attribute.key().equals(key)).findFirst();
    }

    /**
     * Copies this type with a new label and description.
     *
     * @param newLabel       new human-readable label
     * @param newDescription new optional description
     * @return a new type with the supplied label and description
     */
    public UnitTypeDefinition withLabelAndDescription(String newLabel, String newDescription)
    {
        return new UnitTypeDefinition(code, newLabel, newDescription, active, attributes);
    }

    /**
     * Copies this type with a different activation state.
     *
     * @param newActive new activation state
     * @return a new type with the supplied state
     */
    public UnitTypeDefinition withActive(boolean newActive)
    {
        return new UnitTypeDefinition(code, label, description, newActive, attributes);
    }

    /**
     * Copies this type with a different attribute definition list.
     *
     * @param newAttributes replacement attribute definitions
     * @return a new type with the supplied attribute definitions
     */
    public UnitTypeDefinition withAttributes(List<AttributeDefinition> newAttributes)
    {
        return new UnitTypeDefinition(code, label, description, active, newAttributes);
    }
}
