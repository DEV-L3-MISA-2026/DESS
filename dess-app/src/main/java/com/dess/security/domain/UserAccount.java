package com.dess.security.domain;

import java.util.Objects;
import java.util.Set;

/**
 * A user account.
 *
 * <p>It stores the login name, display name, password hash, active flag and roles. The
 * password hash is never copied to the HTTP session.</p>
 *
 * @param id stable identifier
 * @param username login name, unique in the application
 * @param displayName name shown in the interface
 * @param passwordHash hashed password; never the plain password
 * @param active true when the account may log in
 * @param roles roles granted to the account
 */
public record UserAccount(
    String id,
    String username,
    String displayName,
    String passwordHash,
    boolean active,
    Set<Role> roles)
{
    public UserAccount
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(displayName, "displayName");
        if (id.isBlank() || username.isBlank() || displayName.isBlank())
        {
            throw new IllegalArgumentException("id, username and displayName are required");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
