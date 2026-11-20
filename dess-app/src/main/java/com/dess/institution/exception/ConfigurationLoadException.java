package com.dess.institution.exception;

/**
 * @brief Failure while reading the configuration XML file.
 *
 * <p>This exception covers I/O errors, malformed XML and schema violations. The existing file is
 * never modified.</p>
 *
 * @see MalformedXmlException
 * @see SchemaViolationException
 */
public class ConfigurationLoadException extends InstitutionException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a detail message.
     *
     * @param message human-readable description of the failure
     */
    public ConfigurationLoadException(String message)
    {
        super(message);
    }

    /**
     * Creates an exception with a detail message and a cause.
     *
     * @param message human-readable description of the failure
     * @param cause   underlying failure
     */
    public ConfigurationLoadException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
