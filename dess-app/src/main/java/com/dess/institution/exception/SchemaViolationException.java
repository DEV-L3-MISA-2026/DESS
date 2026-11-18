package com.dess.institution.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * @brief The XML document is well-formed but violates {@code institution.xsd}.
 *
 * <p>The individual violations are kept so that callers can display them.</p>
 */
public class SchemaViolationException extends ConfigurationLoadException
{
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> violations;

    /**
     * Creates an exception describing every schema violation.
     *
     * @param source     source name used in the message
     * @param violations schema violations detected by the parser
     */
    public SchemaViolationException(String source, List<String> violations)
    {
        super("File " + source + " does not comply with the XSD schema: " + String.join("; ", violations));
        this.violations = new ArrayList<>(violations);
    }

    /**
     * Returns all detected schema violations.
     *
     * @return an immutable copy of the violation list
     */
    public List<String> getViolations()
    {
        return List.copyOf(violations);
    }
}
