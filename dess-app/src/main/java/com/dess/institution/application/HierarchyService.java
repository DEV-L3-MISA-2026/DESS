package com.dess.institution.application;

import com.dess.institution.domain.OrganizationalUnit;

import java.util.List;
import java.util.Optional;

/**
 * @brief Reads and modifies the organizational unit tree.
 *
 * <p>Returned lists are immutable copies. Unit identifiers are stable and never change when a unit
 * is moved. Any unit type may be used at the root; only parent-child relations are constrained by
 * the configured allowed pairs.</p>
 *
 * <p>Methods that accept a unit identifier throw
 * {@link com.dess.institution.exception.NotFoundException} when the unit does not exist, except
 * {@link #getById(String)} and {@link #getByCode(String)}, which return an optional.</p>
 */
public interface HierarchyService
{
    /**
     * Finds a unit by identifier.
     *
     * @param id unit identifier
     * @return the unit, or an empty optional when it does not exist
     */
    Optional<OrganizationalUnit> getById(String id);

    /**
     * Finds a unit by business code.
     *
     * @param code unit code
     * @return the unit, or an empty optional when it does not exist
     */
    Optional<OrganizationalUnit> getByCode(String code);

    /**
     * Returns all root units.
     *
     * @return an immutable list of units without a parent
     */
    List<OrganizationalUnit> getRoots();

    /**
     * Returns the direct children of a unit.
     *
     * @param id unit identifier
     * @return an immutable list of direct children
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    List<OrganizationalUnit> getChildren(String id);

    /**
     * Returns the parent of a unit.
     *
     * @param id unit identifier
     * @return the parent, or an empty optional for a root unit
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    Optional<OrganizationalUnit> getParent(String id);

    /**
     * Returns every descendant of a unit in depth-first order.
     *
     * @param id unit identifier
     * @return an immutable list of descendants, excluding the unit itself
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    List<OrganizationalUnit> getDescendants(String id);

    /**
     * Returns the ancestors of a unit, from its parent up to the root.
     *
     * @param id unit identifier
     * @return an immutable list of ancestors
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    List<OrganizationalUnit> getAncestors(String id);

    /**
     * Returns the path from the root to a unit, including the unit itself.
     *
     * @param id unit identifier
     * @return an immutable list ordered from the root to the unit
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    List<OrganizationalUnit> getPath(String id);

    /**
     * Returns all units of a given type, whether active or inactive.
     *
     * @param typeCode code of the unit type
     * @return an immutable list of matching units
     */
    List<OrganizationalUnit> getByType(String typeCode);

    /**
     * Returns a unit followed by all of its descendants.
     *
     * @param id unit identifier
     * @return an immutable list starting with the unit itself
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    List<OrganizationalUnit> getSubtree(String id);

    /**
     * Tests whether one unit is a strict descendant of another.
     *
     * @param candidateId identifier of the possible descendant
     * @param ancestorId  identifier of the possible ancestor
     * @return {@code true} when {@code candidateId} is a strict descendant of {@code ancestorId}
     * @throws com.dess.institution.exception.NotFoundException if either unit does not exist
     */
    boolean isDescendantOf(String candidateId, String ancestorId);

    /**
     * Tests whether two units belong to the same branch.
     *
     * @param idA first unit identifier
     * @param idB second unit identifier
     * @return {@code true} when the units are identical or one is an ancestor of the other
     * @throws com.dess.institution.exception.NotFoundException if either unit does not exist
     */
    boolean areInSameBranch(String idA, String idB);

    /**
     * Finds the nearest common ancestor of two units. A unit is considered its own ancestor, so when
     * one unit is an ancestor of the other, that unit is returned.
     *
     * @param idA first unit identifier
     * @param idB second unit identifier
     * @return the nearest common ancestor, or an empty optional when the units belong to distinct trees
     * @throws com.dess.institution.exception.NotFoundException if either unit does not exist
     */
    Optional<OrganizationalUnit> getNearestCommonAncestor(String idA, String idB);

    /**
     * Creates a unit. Attribute values are validated against the unit type and defaults are applied to
     * attributes that were not supplied.
     *
     * @param command creation data
     * @return the created unit
     * @throws com.dess.institution.exception.ValidationException if the type is unknown or inactive, the
     *                                                           parent-child relation is not allowed, a code is
     *                                                           duplicated, a value is invalid or a required
     *                                                           attribute is missing
     */
    OrganizationalUnit createUnit(CreateUnitCommand command);

    /**
     * Updates the editable details of a unit.
     *
     * @param id      unit identifier
     * @param command fields to update; {@code null} fields keep their current value
     * @return the updated unit
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     * @throws com.dess.institution.exception.ValidationException if the resulting configuration is invalid
     */
    OrganizationalUnit updateUnit(String id, UpdateUnitCommand command);

    /**
     * Moves a unit under a new parent without changing its identifier.
     *
     * @param id          unit identifier
     * @param newParentId identifier of the new parent, or {@code null} to make the unit a root
     * @return the moved unit
     * @throws com.dess.institution.exception.NotFoundException if the unit or the new parent does not exist
     * @throws com.dess.institution.exception.ValidationException if the relation is not allowed, the move
     *                                                           would create a cycle, or the unit would become
     *                                                           its own parent or a child of its descendant
     */
    OrganizationalUnit moveUnit(String id, String newParentId);

    /**
     * Deactivates a unit.
     *
     * @param id      unit identifier
     * @param cascade when {@code true}, all descendants are deactivated as well; when {@code false}, the
     *                operation fails if the unit has active children
     * @return the deactivated unit
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     * @throws com.dess.institution.exception.ValidationException if the unit has active children and
     *                                                           {@code cascade} is {@code false}
     */
    OrganizationalUnit deactivateUnit(String id, boolean cascade);

    /**
     * Reactivates a unit.
     *
     * @param id unit identifier
     * @return the reactivated unit
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     * @throws com.dess.institution.exception.ValidationException if the parent unit is inactive
     */
    OrganizationalUnit activateUnit(String id);

    /**
     * Deletes a unit that has no children.
     *
     * @param id unit identifier
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     * @throws com.dess.institution.exception.ValidationException if the unit still has children
     */
    void deleteUnit(String id);
}
