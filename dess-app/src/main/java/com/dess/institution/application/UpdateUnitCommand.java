package com.dess.institution.application;

import java.util.Map;

/**
 * Modification d'une unité. Un champ {@code null} reste inchangé ; l'identifiant, le type et la
 * position dans l'arbre ne se modifient jamais ici (le déplacement passe par {@code moveUnit}).
 *
 * @param code        nouveau code, ou {@code null}
 * @param name        nouveau nom, ou {@code null}
 * @param description nouvelle description, ou {@code null} pour la conserver
 * @param values      si non {@code null}, remplace l'ensemble des valeurs d'attributs
 */
public record UpdateUnitCommand(String code, String name, String description, Map<String, String> values) {
}
