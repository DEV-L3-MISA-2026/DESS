package com.dess.institution.domain;

/**
 * Relation hiérarchique autorisée entre deux types d'unités : une unité de type {@code child}
 * peut avoir pour parent une unité de type {@code parent}.
 *
 * @param parent code du type parent
 * @param child  code du type enfant
 */
public record AllowedParentChildPair(String parent, String child) {

    public AllowedParentChildPair {
        parent = Texts.orEmpty(parent);
        child = Texts.orEmpty(child);
    }
}
