package com.dess.institution.application;

import java.util.List;

/**
 * @brief Result of a side-effect-free validation.
 *
 * @param errors detected violations; empty when the checked data is valid
 */
public record ValidationReport(List<String> errors)
{
    /**
     * Creates a report and defensively copies the error list.
     *
     * @param errors detected violations; {@code null} is treated as an empty list
     */
    public ValidationReport
    {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    /**
     * Indicates whether the checked data is valid.
     *
     * @return {@code true} when no violation was found
     */
    public boolean valid()
    {
        return errors.isEmpty();
    }
}
