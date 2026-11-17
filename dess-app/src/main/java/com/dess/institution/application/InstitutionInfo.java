package com.dess.institution.application;

/**
 * Informations générales de l'établissement.
 *
 * @param id          identifiant technique (immuable)
 * @param code        code métier
 * @param name        nom
 * @param description description facultative (peut être {@code null})
 */
public record InstitutionInfo(String id, String code, String name, String description) {
}
