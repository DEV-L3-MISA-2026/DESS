package com.dess.institution.exception;

/**
 * @brief The configuration file is not well-formed XML.
 *
 * <p>Typical causes are mismatched tags and forbidden DOCTYPE declarations.</p>
 */
public class MalformedXmlException extends ConfigurationLoadException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a detail message and a cause.
     *
     * @param message human-readable description of the failure
     * @param cause   underlying parsing failure
     */
    public MalformedXmlException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
