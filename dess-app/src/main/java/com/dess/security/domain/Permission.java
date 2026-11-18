package com.dess.security.domain;

/**
 * A permission is one allowed action, identified by a code.
 *
 * <p>This is a skeleton. The final code format and validation will be decided later.</p>
 *
 * @param code unique permission code
 * @param description short description of the action
 */
public record Permission(String code, String description)
{
}
