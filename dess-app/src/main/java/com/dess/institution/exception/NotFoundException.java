package com.dess.institution.exception;

/**
 * @brief A requested entity does not exist.
 *
 * <p>Raised when a unit, a unit type, an attribute or an allowed relation cannot be found.</p>
 */
public class NotFoundException extends InstitutionException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a detail message.
     *
     * @param message human-readable description of the missing entity
     */
    public NotFoundException(String message)
    {
        super(message);
    }
}
