package com.dess.institution.application;

/**
 * @brief General information about the institution.
 *
 * @param id          immutable technical identifier
 * @param code        business code
 * @param name        display name
 * @param description optional description; may be {@code null}
 */
public record InstitutionInfo(String id, String code, String name, String description)
{
}
