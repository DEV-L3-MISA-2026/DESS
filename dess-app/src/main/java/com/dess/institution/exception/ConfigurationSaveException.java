package com.dess.institution.exception;

/**
 * @brief Failure while writing the configuration XML file.
 *
 * <p>When this exception is thrown, the previous file and the in-memory configuration remain
 * unchanged.</p>
 */
public class ConfigurationSaveException extends InstitutionException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a detail message.
     *
     * @param message human-readable description of the failure
     */
    public ConfigurationSaveException(String message)
    {
        super(message);
    }

    /**
     * Creates an exception with a detail message and a cause.
     *
     * @param message human-readable description of the failure
     * @param cause   underlying failure
     */
    public ConfigurationSaveException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
