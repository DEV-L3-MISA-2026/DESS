package com.dess.institution.exception;

/**
 * @brief Root of every unchecked exception thrown by the institution module.
 */
public class InstitutionException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a detail message.
     *
     * @param message human-readable description of the failure
     */
    public InstitutionException(String message)
    {
        super(message);
    }

    /**
     * Creates an exception with a detail message and a cause.
     *
     * @param message human-readable description of the failure
     * @param cause   underlying failure
     */
    public InstitutionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
