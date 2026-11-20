package com.dess.security.application;

import com.dess.security.domain.UserAccount;

import java.util.Optional;

/**
 * Checks a user's login.
 *
 * <p>The implementation loads the account, checks the password with the password adapter and
 * returns the account when the login is valid. A failed login does not reveal whether the
 * username exists.</p>
 */
public interface AuthenticationService
{
    /**
     * Checks a username and password.
     *
     * @param username login name
     * @param rawPassword plain password from the user; it must not be stored
     * @return the account when the login is valid, or an empty result when it is not
     */
    Optional<UserAccount> authenticate(String username, String rawPassword);
}
