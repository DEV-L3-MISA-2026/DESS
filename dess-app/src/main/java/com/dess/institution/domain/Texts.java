package com.dess.institution.domain;

import java.util.Objects;

/**
 * @brief Small text-normalization helpers shared by the domain model.
 */
public final class Texts
{
    private Texts()
    {
    }

    /**
     * Returns the given text, or an empty string when it is {@code null}. Validation later reports
     * required fields that remain empty.
     *
     * @param value text to normalize, possibly {@code null}
     * @return the original text, or an empty string
     */
    public static String orEmpty(String value)
    {
        return Objects.toString(value, "");
    }

    /**
     * Converts a {@code null} or blank text to {@code null}.
     *
     * @param value text to normalize
     * @return {@code null} when the text is {@code null} or blank, otherwise the original text
     */
    public static String blankToNull(String value)
    {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Indicates whether a text is {@code null} or blank.
     *
     * @param value text to test
     * @return {@code true} when the text is {@code null} or contains only whitespace
     */
    public static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }
}
