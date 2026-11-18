package com.dess.security.application;

import com.dess.security.domain.UserAccount;

import java.util.Optional;

/**
 * @brief Verifies user credentials.
 *
 * <p>Scaffold only: the interface declares the authentication use case. An implementation will
 * load the account, verify the password through the password adapter and return the account when
 * authentication succeeds. A failure must not reveal whether the username exists.</p>
 */
public interface AuthenticationService
{
    /**
     * Authenticates a user.
     *
     * @param username    login name
     * @param rawPassword plain-text password supplied by the user; implementations must not retain it
     * @return the authenticated account, or an empty optional when the credentials are invalid or
     *         the account is inactive
     */
    Optional<UserAccount> authenticate(String username, String rawPassword);
}
