package com.dess.institution.application;

import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @brief Manages custom attribute definitions per unit type and attribute values per unit.
 *
 * <p>Every modification is validated against the complete institution configuration. An operation
 * therefore either succeeds with a valid configuration or fails without changing the stored state.</p>
 *
 * @see UnitTypeService
 */
public interface AttributeService
{
    /**
     * Returns the attribute definitions declared for a unit type.
     *
     * @param typeCode code of the unit type
     * @return an immutable list of definitions, possibly empty
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     */
    List<AttributeDefinition> getDefinitions(String typeCode);

    /**
     * Adds an attribute definition to a unit type.
     *
     * <p>When the attribute is required and has a default value, existing units of the type receive
     * that default. Otherwise, adding a required attribute to a type that already has units is
     * rejected because those units would become invalid.</p>
     *
     * @param typeCode   code of the unit type
     * @param definition definition to add
     * @return the added definition
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     * @throws com.dess.institution.exception.ValidationException if the key is already defined or a rule is violated
     */
    AttributeDefinition addDefinition(String typeCode, AttributeDefinition definition);

    /**
     * Replaces the definition with the same key. Attribute keys are immutable.
     *
     * @param typeCode   code of the unit type
     * @param definition new definition
     * @return the updated definition
     * @throws com.dess.institution.exception.NotFoundException if the type or attribute does not exist
     * @throws com.dess.institution.exception.ValidationException if existing values would become invalid
     */
    AttributeDefinition updateDefinition(String typeCode, AttributeDefinition definition);

    /**
     * Removes an attribute definition.
     *
     * @param typeCode code of the unit type
     * @param key      attribute key
     * @throws com.dess.institution.exception.NotFoundException if the type or attribute does not exist
     * @throws com.dess.institution.exception.ValidationException if at least one unit still holds a value for the key
     */
    void removeDefinition(String typeCode, String key);

    /**
     * Returns all attribute values of a unit.
     *
     * @param unitId identifier of the organizational unit
     * @return an immutable list of values, possibly empty
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    List<AttributeValue> getValues(String unitId);

    /**
     * Returns the serialized value of one attribute of a unit.
     *
     * @param unitId identifier of the organizational unit
     * @param key    attribute key
     * @return the serialized value, or an empty optional when the unit has no value for the key
     * @throws com.dess.institution.exception.NotFoundException if the unit does not exist
     */
    Optional<String> getValue(String unitId, String key);

    /**
     * Returns a value converted to the Java representation of its attribute type.
     *
     * @param unitId identifier of the organizational unit
     * @param key    attribute key
     * @return a {@code String}, {@code Long}, {@code BigDecimal}, {@code Boolean} or {@code LocalDate},
     *         or an empty optional when the unit has no value for the key
     * @throws com.dess.institution.exception.NotFoundException if the unit or attribute does not exist
     */
    Optional<Object> getTypedValue(String unitId, String key);

    /**
     * Sets or replaces the value of an attribute for a unit after validation.
     *
     * @param unitId identifier of the organizational unit
     * @param key    attribute key
     * @param value  non-blank serialized value
     * @return the stored value
     * @throws com.dess.institution.exception.NotFoundException if the unit or attribute does not exist
     * @throws com.dess.institution.exception.ValidationException if the value is blank or invalid for its type
     */
    AttributeValue setValue(String unitId, String key, String value);

    /**
     * Removes an attribute value from a unit.
     *
     * @param unitId identifier of the organizational unit
     * @param key    attribute key
     * @throws com.dess.institution.exception.NotFoundException if the unit or value does not exist
     * @throws com.dess.institution.exception.ValidationException if the attribute is required
     */
    void removeValue(String unitId, String key);

    /**
     * Checks a single value without storing it.
     *
     * @param typeCode code of the unit type
     * @param key      attribute key
     * @param value    serialized value to check
     * @return the validation report; {@link ValidationReport#valid()} is {@code true} when the value is acceptable
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     */
    ValidationReport validateValue(String typeCode, String key, String value);

    /**
     * Checks a complete set of values for a unit type without storing anything. This is typically used
     * before submitting a unit creation form.
     *
     * @param typeCode code of the unit type
     * @param values   serialized values keyed by attribute key
     * @return the validation report listing unknown attributes, invalid values and missing required attributes
     * @throws com.dess.institution.exception.NotFoundException if the type does not exist
     */
    ValidationReport validateValues(String typeCode, Map<String, String> values);
}
