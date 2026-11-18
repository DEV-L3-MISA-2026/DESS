package com.dess.institution.application;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.UnitTypeDefinition;

import java.util.List;
import java.util.Optional;

/**
 * @brief Manages user-defined unit types and the parent-child relations allowed between them.
 *
 * <p>Attribute definitions of a unit type are managed through {@link AttributeService}.</p>
 */
public interface UnitTypeService
{
    /**
     * Creates a unit type. The code must be unique; the type may already declare attribute definitions.
     *
     * @param type type to create
     * @return the created type
     * @throws com.dess.institution.exception.ValidationException if the code already exists or is invalid
     */
    UnitTypeDefinition create(UnitTypeDefinition type);

    /**
     * Updates the label and description of a unit type. The code is immutable.
     *
     * @param code        code of the type to update
     * @param label       new label
     * @param description new optional description
     * @return the updated type
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     * @throws com.dess.institution.exception.ValidationException if the resulting configuration is invalid
     */
    UnitTypeDefinition update(String code, String label, String description);

    /**
     * Finds a unit type by code.
     *
     * @param code type code
     * @return the type, or an empty optional when it does not exist
     */
    Optional<UnitTypeDefinition> getByCode(String code);

    /**
     * Returns all unit types.
     *
     * @return an immutable list of types
     */
    List<UnitTypeDefinition> getAll();

    /**
     * Deactivates a unit type. Inactive types no longer accept new units; existing units are kept.
     *
     * @param code type code
     * @return the deactivated type
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     */
    UnitTypeDefinition deactivate(String code);

    /**
     * Activates a unit type.
     *
     * @param code type code
     * @return the activated type
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     */
    UnitTypeDefinition activate(String code);

    /**
     * Deletes a unit type.
     *
     * @param code type code
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     * @throws com.dess.institution.exception.ValidationException if units still use the type or allowed
     *                                                           parent-child relations still reference it
     */
    void delete(String code);

    /**
     * Returns all allowed parent-child type relations.
     *
     * @return an immutable list of allowed pairs
     */
    List<AllowedParentChildPair> getAllowedPairs();

    /**
     * Allows units of {@code childType} to be created under units of {@code parentType}.
     *
     * @param parentType parent type code
     * @param childType  child type code
     * @return the added relation
     * @throws com.dess.institution.exception.ValidationException if the relation is invalid or duplicated
     */
    AllowedParentChildPair addAllowedPair(String parentType, String childType);

    /**
     * Removes an allowed parent-child type relation.
     *
     * @param parentType parent type code
     * @param childType  child type code
     * @throws com.dess.institution.exception.NotFoundException if the relation does not exist
     * @throws com.dess.institution.exception.ValidationException if existing units rely on the relation
     */
    void removeAllowedPair(String parentType, String childType);

    /**
     * Tests whether a parent-child relation between two unit types is allowed.
     *
     * @param parentType parent type code
     * @param childType  child type code
     * @return {@code true} when the relation is allowed
     */
    boolean isAllowed(String parentType, String childType);

    /**
     * Returns the child type codes accepted under a parent type.
     *
     * @param parentType parent type code
     * @return an immutable list of child type codes
     */
    List<String> getAllowedChildTypes(String parentType);

    /**
     * Returns the parent type codes that accept a child type.
     *
     * @param childType child type code
     * @return an immutable list of parent type codes
     */
    List<String> getAllowedParentTypes(String childType);
}
