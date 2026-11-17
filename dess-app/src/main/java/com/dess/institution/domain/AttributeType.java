package com.dess.institution.domain;

/**
 * Types de données disponibles pour un attribut personnalisé.
 * <p>
 * Il s'agit des types de <em>données</em> (texte, nombre, date...), pas des types d'unités
 * organisationnelles : ces derniers sont définis dynamiquement par l'utilisateur.
 */
public enum AttributeType {
    /** Texte libre. */
    TEXT,
    /** Entier (représenté par un {@code long}). */
    NUMBER,
    /** Nombre décimal (représenté par un {@link java.math.BigDecimal}). */
    DECIMAL,
    /** {@code true} ou {@code false} uniquement. */
    BOOLEAN,
    /** Date ISO {@code yyyy-MM-dd}. */
    DATE,
    /** Adresse e-mail. */
    EMAIL,
    /** URL absolue http ou https. */
    URL
}
