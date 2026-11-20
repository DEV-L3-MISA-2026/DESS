package com.dess.security.domain;

import java.util.Set;

/**
 * A user account.
 *
 * <p>It stores the login name, display name, password hash, active flag and roles. This is a
 * skeleton: validation and defensive copying are not added yet.</p>
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
}
