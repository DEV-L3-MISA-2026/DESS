package com.dess.security.domain;

import java.util.Objects;

/**
 * A permission is one allowed action, identified by a code.
 *
 * @param code unique permission code
 * @param description short description of the action
 */
public record Permission(String code, String description)
{
    public Permission
    {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(description, "description");
        if (code.isBlank())
        {
            throw new IllegalArgumentException("A permission code is required");
        }
    }
}
