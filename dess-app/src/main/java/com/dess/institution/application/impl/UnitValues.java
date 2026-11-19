package com.dess.institution.application.impl;

import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.UnitTypeDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @brief Normalizes submitted attribute values before validation.
 *
 * <p>Blank values are treated as absent, values are stripped, and defaults are applied to attributes
 * that were not supplied. Unknown keys are preserved so that the validator can report them.</p>
 */
final class UnitValues
{
    private UnitValues()
    {
    }

    /**
     * Builds the attribute value map of a unit from user input.
     *
     * @param type  unit type providing the attribute definitions
     * @param input submitted values keyed by attribute key; may be {@code null}
     * @return a mutable map of normalized values, ready for validation
     */
    static Map<String, AttributeValue> build(UnitTypeDefinition type, Map<String, String> input)
    {
        Map<String, AttributeValue> values = new LinkedHashMap<>();

        if (input != null)
        {
            input.forEach((key, rawValue) ->
            {
                if (rawValue != null && !rawValue.isBlank())
                {
                    values.put(key, new AttributeValue(key, rawValue.strip()));
                }
            });
        }

        for (AttributeDefinition definition : type.attributes())
        {
            if (definition.hasDefault() && !values.containsKey(definition.key()))
            {
                values.put(
                    definition.key(),
                    new AttributeValue(definition.key(), definition.defaultValue())
                );
            }
        }

        return values;
    }
}
