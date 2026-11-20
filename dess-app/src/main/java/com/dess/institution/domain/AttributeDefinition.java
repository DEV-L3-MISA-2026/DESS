package com.dess.institution.domain;

import java.util.Objects;

/**
 * @brief Definition of a custom attribute available to every unit of a given type.
 *
 * <p>The actual value stored for a concrete unit is an {@link AttributeValue}.</p>
 *
 * @param key          key, unique within its unit type (for example {@code headOfDepartment})
 * @param label        human-readable label
 * @param description  optional description
 * @param type         data type used to validate values
 * @param required     {@code true} when every unit of the type must provide a value
 * @param defaultValue optional serialized default value, applied when a unit is created
 */
public record AttributeDefinition(
    String key,
    String label,
    String description,
    AttributeType type,
    boolean required,
    String defaultValue)
{
    /**
     * Normalizes text fields and rejects a missing data type.
     *
     * @param key          attribute key
     * @param label        human-readable label
     * @param description  optional description
     * @param type         data type
     * @param required     whether the attribute is mandatory
     * @param defaultValue optional serialized default value
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public AttributeDefinition
    {
        key = Texts.orEmpty(key);
        label = Texts.orEmpty(label);
        description = Texts.blankToNull(description);
        type = Objects.requireNonNull(type, "type");
        defaultValue = Texts.blankToNull(defaultValue);
    }

    /**
     * Creates a definition without description or default value.
     *
     * @param key       attribute key
     * @param label     human-readable label
     * @param type      data type
     * @param required  whether the attribute is mandatory
     * @return the new definition
     */
    public static AttributeDefinition of(String key, String label, AttributeType type, boolean required)
    {
        return new AttributeDefinition(key, label, null, type, required, null);
    }

    /**
     * Creates a definition with a default value and no description.
     *
     * @param key          attribute key
     * @param label        human-readable label
     * @param type         data type
     * @param required     whether the attribute is mandatory
     * @param defaultValue serialized default value
     * @return the new definition
     */
    public static AttributeDefinition of(String key, String label, AttributeType type, boolean required,
                                         String defaultValue)
    {
        return new AttributeDefinition(key, label, null, type, required, defaultValue);
    }

    /**
     * Indicates whether a default value is defined.
     *
     * @return {@code true} when {@code defaultValue} is not {@code null}
     */
    public boolean hasDefault()
    {
        return defaultValue != null;
    }
}
