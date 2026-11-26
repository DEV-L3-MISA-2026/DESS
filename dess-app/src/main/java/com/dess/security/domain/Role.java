package com.dess.security.domain;

import java.util.Objects;
import java.util.Set;

/**
 * A role is a named group of permissions.
 *
 * @param code unique role code
 * @param name name shown in the interface
 * @param permissions permissions granted by the role
 */
public record Role(String code, String name, Set<Permission> permissions)
{
    public Role
    {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        if (code.isBlank())
        {
            throw new IllegalArgumentException("A role code is required");
        }
        if (name.isBlank())
        {
            throw new IllegalArgumentException("A role name is required");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
