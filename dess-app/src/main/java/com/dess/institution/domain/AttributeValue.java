package com.dess.institution.domain;

/**
 * @brief Serialized value of one attribute for a concrete organizational unit.
 *
 * <p>The value is kept as text. Conversion and validation are provided by the attribute's
 * {@link AttributeType}.</p>
 *
 * @param key   key of the corresponding {@link AttributeDefinition}
 * @param value serialized value
 */
public record AttributeValue(String key, String value)
{
    /**
     * Normalizes {@code null} fields to empty strings.
     *
     * @param key   attribute key
     * @param value serialized value
     */
    public AttributeValue
    {
        key = Texts.orEmpty(key);
        value = Texts.orEmpty(value);
    }
}
