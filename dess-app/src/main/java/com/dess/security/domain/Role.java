package com.dess.security.domain;

import java.util.Set;

/**
 * @brief Named set of permissions granted to a user account.
 *
 * <p>Scaffold only: the record declares the role data. Uniqueness and defensive copying will be
 * added with the implementation.</p>
 *
 * @param code        unique role code
 * @param name        human-readable name
 * @param permissions permissions granted by the role
 */
public record Role(String code, String name, Set<Permission> permissions)
{
}
