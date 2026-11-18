package com.dess.institution.application;

import java.util.Map;

/**
 * @brief Editable fields of an organizational unit.
 *
 * <p>A {@code null} field keeps its current value. The identifier, the unit type and the position
 * in the tree cannot be changed here; use {@link HierarchyService#moveUnit(String, String)} to move
 * a unit.</p>
 *
 * @param code        new business code, or {@code null} to keep the current one
 * @param name        new display name, or {@code null} to keep the current one
 * @param description new description, or {@code null} to keep the current one
 * @param values      when non-{@code null}, replaces all custom attribute values
 */
public record UpdateUnitCommand(String code, String name, String description, Map<String, String> values)
{
}
