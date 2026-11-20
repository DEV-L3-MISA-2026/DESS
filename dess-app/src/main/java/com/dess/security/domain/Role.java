package com.dess.security.domain;

import java.util.Set;

/**
 * A role is a named group of permissions.
 *
 * <p>This is a skeleton: validation and defensive copying are not added yet.</p>
 *
 * @param code unique role code
 * @param name name shown in the interface
 * @param permissions permissions granted by the role
 */
public record Role(String code, String name, Set<Permission> permissions)
{
}
