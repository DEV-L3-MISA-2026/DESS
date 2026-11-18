package com.dess.security.domain;

/**
 * @brief Permission that can be granted through a role.
 *
 * <p>Scaffold only: the record declares the permission data. The final code format and validation
 * will be defined with the implementation.</p>
 *
 * @param code        unique permission code
 * @param description human-readable description
 */
public record Permission(String code, String description)
{
}
