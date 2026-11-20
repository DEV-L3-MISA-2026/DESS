package com.dess.security.application.spi;

/**
 * Hashes and verifies passwords.
 *
 * <p>The application layer calls this port so it does not depend on a concrete
 * algorithm. The adapter must use a per-user salt and a constant-time
 * comparison.</p>
 */
public interface PasswordEncoder
{
    /**
     * Hashes a plain password.
     *
     * @param rawPassword plain password; it is never stored
     * @return an encoded value that contains the algorithm, salt and hash
     */
    String encode(String rawPassword);

    /**
     * Verifies a plain password against an encoded value.
     *
     * @param rawPassword plain password from the user
     * @param encodedPassword value produced by {@link #encode(String)}
     * @return true when the password matches
     */
    boolean matches(String rawPassword, String encodedPassword);
}
