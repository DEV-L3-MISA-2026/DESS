package com.dess.institution.application;

import java.util.Map;

/**
 * @brief Data required to create an organizational unit.
 *
 * @param id          requested identifier; when {@code null} or blank, a UUID is generated
 * @param code        business code, unique within the institution
 * @param name        display name
 * @param description optional description
 * @param typeCode    code of the unit type; it must exist and be active
 * @param parentId    identifier of the parent unit, or {@code null} to create a root unit
 * @param active      initial activation state
 * @param values      custom attribute values keyed by attribute key; may be {@code null}
 */
public record CreateUnitCommand(
    String id,
    String code,
    String name,
    String description,
    String typeCode,
    String parentId,
    boolean active,
    Map<String, String> values)
{
    /**
     * Creates a command for an active unit without an imposed identifier or description.
     *
     * @param code     business code, unique within the institution
     * @param name     display name
     * @param typeCode code of the unit type
     * @param parentId identifier of the parent unit, or {@code null} for a root unit
     * @param values   custom attribute values keyed by attribute key; may be {@code null}
     * @return a create command with a generated identifier and no description
     */
    public static CreateUnitCommand of(String code, String name, String typeCode, String parentId,
                                       Map<String, String> values)
    {
        return new CreateUnitCommand(null, code, name, null, typeCode, parentId, true, values);
    }
}
