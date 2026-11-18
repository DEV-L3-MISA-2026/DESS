package com.dess.security.domain;

import java.util.Set;

/**
 * @brief Account that can authenticate against the application.
 *
 * <p>Scaffold only: the record declares the data carried by an account. Validation, normalization
 * and defensive copying will be added with the implementation. The password hash is an opaque
 * value produced by the password adapter and must never be exposed by the web layer.</p>
 *
 * @param id           stable technical identifier
 * @param username     login name, unique within the application
 * @param displayName  name shown in the interface
 * @param passwordHash encoded password; never the plain-text password
 * @param active       whether the account may authenticate
 * @param roles        roles granted to the account
 */
public record UserAccount(
    String id,
    String username,
    String displayName,
    String passwordHash,
    boolean active,
    Set<Role> roles)
{
}
