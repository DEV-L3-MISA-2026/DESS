package com.dess.institution.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @brief Immutable snapshot of the complete institution configuration.
 *
 * <p>The snapshot contains the general institution information, the unit types, the allowed
 * parent-child relations and the flat list of organizational units. The tree structure is carried
 * by {@link OrganizationalUnit#parentId()}. Every modification returns a new instance, which allows
 * a complete state to be validated before it is persisted and published.</p>
 */
public record InstitutionConfig(
    String id,
    String code,
    String name,
    String description,
    List<UnitTypeDefinition> unitTypes,
    List<AllowedParentChildPair> allowedPairs,
    List<OrganizationalUnit> units)
{
    /**
     * Normalizes text fields and defensively copies collections.
     *
     * @param id           immutable technical identifier
     * @param code         business code
     * @param name         display name
     * @param description  optional description
     * @param unitTypes    unit type definitions; {@code null} is treated as an empty list
     * @param allowedPairs allowed parent-child relations; {@code null} is treated as an empty list
     * @param units        organizational units; {@code null} is treated as an empty list
     */
    public InstitutionConfig
    {
        id = Texts.orEmpty(id);
        code = Texts.orEmpty(code);
        name = Texts.orEmpty(name);
        description = Texts.blankToNull(description);
        unitTypes = unitTypes == null ? List.of() : List.copyOf(unitTypes);
        allowedPairs = allowedPairs == null ? List.of() : List.copyOf(allowedPairs);
        units = units == null ? List.of() : List.copyOf(units);
    }

    /**
     * Creates an empty configuration.
     *
     * @param id          immutable technical identifier
     * @param code        business code
     * @param name        display name
     * @param description optional description
     * @return a configuration without unit types, relations or units
     */
    public static InstitutionConfig empty(String id, String code, String name, String description)
    {
        return new InstitutionConfig(id, code, name, description, List.of(), List.of(), List.of());
    }

    /**
     * Finds a unit type by code.
     *
     * @param typeCode type code
     * @return the type, or an empty optional when it does not exist
     */
    public Optional<UnitTypeDefinition> findType(String typeCode)
    {
        return unitTypes.stream().filter(t -> t.code().equals(typeCode)).findFirst();
    }

    /**
     * Finds an organizational unit by identifier.
     *
     * @param unitId unit identifier
     * @return the unit, or an empty optional when it does not exist
     */
    public Optional<OrganizationalUnit> findUnit(String unitId)
    {
        return units.stream().filter(u -> u.id().equals(unitId)).findFirst();
    }

    /**
     * Finds an organizational unit by business code.
     *
     * <p>A code is unique only among the children of one parent. When several units share the code,
     * the first one in file order is returned; use the identifier for an exact reference.</p>
     *
     * @param unitCode unit code, compared exactly
     * @return the first matching unit, or an empty optional when none exists
     */
    public Optional<OrganizationalUnit> findUnitByCode(String unitCode)
    {
        return units.stream().filter(u -> u.code().equals(unitCode)).findFirst();
    }

    /**
     * Tests whether a parent-child relation between two unit types is allowed.
     *
     * @param parentTypeCode parent type code
     * @param childTypeCode  child type code
     * @return {@code true} when the relation is declared
     */
    public boolean isAllowed(String parentTypeCode, String childTypeCode)
    {
        return allowedPairs.stream()
                           .anyMatch(p -> p.parent().equals(parentTypeCode) && p.child().equals(childTypeCode));
    }

    /**
     * Returns the direct children of a unit.
     *
     * @param unitId unit identifier
     * @return an immutable list of direct children
     */
    public List<OrganizationalUnit> childrenOf(String unitId)
    {
        return units.stream().filter(u -> unitId.equals(u.parentId())).toList();
    }

    /**
     * Builds an index from parent identifier to children, preserving declaration order.
     *
     * @return an immutable map whose values are immutable lists
     */
    public Map<String, List<OrganizationalUnit>> childrenIndex()
    {
        Map<String, List<OrganizationalUnit>> index = new HashMap<>();

        for (OrganizationalUnit unit : units)
        {
            if (unit.parentId() != null)
            {
                index.computeIfAbsent(unit.parentId(), ignored -> new ArrayList<>()).add(unit);
            }
        }

        index.replaceAll((parentId, children) -> List.copyOf(children));

        return Map.copyOf(index);
    }

    /**
     * Returns the ancestors of a unit, from its parent up to the root.
     *
     * @param unit unit whose ancestors are requested
     * @return an immutable list ordered from the nearest ancestor to the farthest
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    public List<OrganizationalUnit> ancestorsOf(OrganizationalUnit unit)
    {
        Objects.requireNonNull(unit, "unit");

        List<OrganizationalUnit> ancestors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(unit.id());
        OrganizationalUnit current = unit;

        while (current.parentId() != null)
        {
            OrganizationalUnit parent = findUnit(current.parentId()).orElse(null);

            if (parent == null || !visited.add(parent.id()))
            {
                break;
            }

            ancestors.add(parent);
            current = parent;
        }

        return List.copyOf(ancestors);
    }

    /**
     * Returns the descendants of a unit in depth-first order.
     *
     * @param unitId unit identifier
     * @return an immutable list of descendants, excluding the unit itself
     */
    public List<OrganizationalUnit> descendantsOf(String unitId)
    {
        List<OrganizationalUnit> descendants = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(unitId);

        collectDescendants(childrenIndex(), unitId, descendants, visited);

        return List.copyOf(descendants);
    }

    /**
     * Returns the path from the root to a unit, including the unit itself.
     *
     * @param unit unit whose path is requested
     * @return an immutable list ordered from the root to the unit
     * @throws NullPointerException if {@code unit} is {@code null}
     */
    public List<OrganizationalUnit> pathOf(OrganizationalUnit unit)
    {
        List<OrganizationalUnit> path = new ArrayList<>(ancestorsOf(unit));
        Collections.reverse(path);
        path.add(unit);

        return List.copyOf(path);
    }

    /**
     * Copies this configuration with new general information.
     *
     * @param newCode        new business code
     * @param newName        new display name
     * @param newDescription new optional description
     * @return a new configuration with the updated information
     */
    public InstitutionConfig withInfo(String newCode, String newName, String newDescription)
    {
        return new InstitutionConfig(id, newCode, newName, newDescription, unitTypes, allowedPairs, units);
    }

    /**
     * Copies this configuration with a different unit type list.
     *
     * @param newTypes replacement unit types
     * @return a new configuration with the supplied unit types
     */
    public InstitutionConfig withUnitTypes(List<UnitTypeDefinition> newTypes)
    {
        return new InstitutionConfig(id, code, name, description, newTypes, allowedPairs, units);
    }

    /**
     * Copies this configuration with a different allowed relation list.
     *
     * @param newPairs replacement relations
     * @return a new configuration with the supplied relations
     */
    public InstitutionConfig withAllowedPairs(List<AllowedParentChildPair> newPairs)
    {
        return new InstitutionConfig(id, code, name, description, unitTypes, newPairs, units);
    }

    /**
     * Copies this configuration with a different unit list.
     *
     * @param newUnits replacement units
     * @return a new configuration with the supplied units
     */
    public InstitutionConfig withUnits(List<OrganizationalUnit> newUnits)
    {
        return new InstitutionConfig(id, code, name, description, unitTypes, allowedPairs, newUnits);
    }

    /**
     * Replaces a unit, matching by identifier and preserving its position in the list.
     *
     * @param updated updated unit
     * @return a new configuration containing the replacement
     */
    public InstitutionConfig replaceUnit(OrganizationalUnit updated)
    {
        List<OrganizationalUnit> copy = new ArrayList<>(units.size());

        for (OrganizationalUnit unit : units)
        {
            copy.add(unit.id().equals(updated.id()) ? updated : unit);
        }

        return withUnits(copy);
    }

    /**
     * Replaces a unit type, matching by code and preserving its position in the list.
     *
     * @param updated updated unit type
     * @return a new configuration containing the replacement
     */
    public InstitutionConfig replaceType(UnitTypeDefinition updated)
    {
        List<UnitTypeDefinition> copy = new ArrayList<>(unitTypes.size());

        for (UnitTypeDefinition type : unitTypes)
        {
            copy.add(type.code().equals(updated.code()) ? updated : type);
        }

        return withUnitTypes(copy);
    }

    private static void collectDescendants(Map<String, List<OrganizationalUnit>> byParent, String unitId,
                                           List<OrganizationalUnit> out, Set<String> visited)
    {
        for (OrganizationalUnit child : byParent.getOrDefault(unitId, List.of()))
        {
            if (visited.add(child.id()))
            {
                out.add(child);
                collectDescendants(byParent, child.id(), out, visited);
            }
        }
    }
}
