package com.dess.institution.exception;

/**
 * @brief No institution configuration is available yet.
 *
 * <p>The configuration must be supplied explicitly, either as an initial XML file or through
 * {@code InstitutionService#initialize}. The module never invents an institution.</p>
 */
public class NotInitializedException extends InstitutionException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the standard not-initialized message.
     */
    public NotInitializedException()
    {
        super("No institution configuration is loaded: provide the initial XML file "
              + "or initialize the institution.");
    }
}
