package com.dess.institution.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * @brief An operation would violate a business rule.
 *
 * <p>Examples include a forbidden parent-child relation, a duplicated code, an invalid value or a
 * missing required attribute. When this exception is thrown, the configuration remains
 * unchanged.</p>
 */
public class ValidationException extends InstitutionException
{
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> violations;

    /**
     * Creates an exception for a single violation.
     *
     * @param violation human-readable description of the violation
     */
    public ValidationException(String violation)
    {
        this(List.of(violation));
    }

    /**
     * Creates an exception for a list of violations.
     *
     * @param violations human-readable descriptions of the violations
     */
    public ValidationException(List<String> violations)
    {
        super(String.join(" ; ", violations));
        this.violations = new ArrayList<>(violations);
    }

    /**
     * Returns all detected violations.
     *
     * @return an immutable copy of the violation list
     */
    public List<String> getViolations()
    {
        return List.copyOf(violations);
    }
}
