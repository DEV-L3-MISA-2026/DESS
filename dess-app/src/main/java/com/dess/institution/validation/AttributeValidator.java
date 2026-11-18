package com.dess.institution.validation;

import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.Texts;
import com.dess.institution.domain.UnitTypeDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @brief Validation rules for custom attribute definitions and unit attribute values.
 *
 * @see InstitutionValidator
 */
public final class AttributeValidator
{
    /**
     * Attribute key pattern: a letter followed by letters, digits or underscores.
     */
    public static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private AttributeValidator()
    {
    }

    /**
     * Validates the attribute definitions of a unit type.
     *
     * <p>The checks cover well-formed and unique keys, required labels and default values compatible
     * with their declared type.</p>
     *
     * @param type   unit type whose definitions are validated
     * @param errors list receiving the detected violations
     */
    public static void validateDefinitions(UnitTypeDefinition type, List<String> errors)
    {
        Set<String> keys = new HashSet<>();

        for (AttributeDefinition definition : type.attributes())
        {
            String location = "Type '" + type.code() + "', attribute '" + definition.key() + "': ";

            if (!KEY_PATTERN.matcher(definition.key()).matches())
            {
                errors.add(location + "invalid key (letter followed by letters, digits or _)");
            }
            else if (!keys.add(definition.key()))
            {
                errors.add(location + "duplicate key in type");
            }

            if (Texts.isBlank(definition.label()))
            {
                errors.add(location + "label required");
            }

            if (definition.hasDefault())
            {
                definition.type()
                          .check(definition.defaultValue())
                          .ifPresent(message -> errors.add(location + "invalid default value: " + message));
            }
        }
    }

    /**
     * Validates the attribute values of a unit against the definitions of its type.
     *
     * <p>Unknown keys, values incompatible with their data type, and missing or blank required
     * attributes are reported.</p>
     *
     * @param unitLabel label used in error messages, for example "Law (FAC_LAW)"
     * @param type      unit type providing the attribute definitions
     * @param values    values to validate, keyed by attribute key
     * @param errors    list receiving the detected violations
     */
    public static void validateValues(String unitLabel, UnitTypeDefinition type,
                                      Map<String, AttributeValue> values, List<String> errors)
    {
        for (AttributeValue value : values.values())
        {
            Optional<AttributeDefinition> definition = type.findAttribute(value.key());

            if (definition.isEmpty())
            {
                errors.add("Unit " + unitLabel + ": attribute '" + value.key()
                           + "' is not defined for type '" + type.code() + "'");
                continue;
            }

            definition.get()
                      .type()
                      .check(value.value())
                      .ifPresent(message -> errors.add(
                          "Unit " + unitLabel + ", attribute '" + value.key() + "': " + message
                      ));
        }

        for (AttributeDefinition definition : type.attributes())
        {
            if (definition.required())
            {
                AttributeValue value = values.get(definition.key());

                if (value == null || value.value().isBlank())
                {
                    errors.add("Unit " + unitLabel + ": required attribute '" + definition.key()
                               + "' is missing");
                }
            }
        }
    }
}
