package com.dess.institution.domain;

/**
 * @brief Allowed hierarchical relation between two unit types.
 *
 * <p>An instance means that a unit of type {@code child} may have a parent of type {@code parent}.</p>
 *
 * @param parent parent type code
 * @param child  child type code
 */
public record AllowedParentChildPair(String parent, String child)
{
    /**
     * Normalizes {@code null} codes to empty strings so that validation can report them consistently.
     *
     * @param parent parent type code
     * @param child  child type code
     */
    public AllowedParentChildPair
    {
        parent = Texts.orEmpty(parent);
        child = Texts.orEmpty(child);
    }
}
