package com.dess.institution.application;

import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Définitions d'attributs personnalisés (par type d'unité) et valeurs (par unité).
 * <p>
 * Toute modification est validée sur l'ensemble des unités existantes : la configuration ne peut
 * jamais devenir invalide.
 */
public interface AttributeService {

    // ------------------------------------------------------------ définitions

    List<AttributeDefinition> getDefinitions(String typeCode);

    /**
     * Ajoute une définition à un type. Si elle est obligatoire et que le type est déjà utilisé :
     * les unités existantes reçoivent la valeur par défaut si elle est définie ; sinon l'ajout est
     * refusé car les unités existantes deviendraient invalides.
     */
    AttributeDefinition addDefinition(String typeCode, AttributeDefinition definition);

    /**
     * Remplace la définition portant la même clé (la clé est immuable). Refusé si les valeurs
     * existantes deviennent invalides (changement de type de donnée, par exemple).
     */
    AttributeDefinition updateDefinition(String typeCode, AttributeDefinition definition);

    /** Supprime une définition ; refusé si au moins une unité porte encore une valeur pour cette clé. */
    void removeDefinition(String typeCode, String key);

    // ------------------------------------------------------------ valeurs

    List<AttributeValue> getValues(String unitId);

    Optional<String> getValue(String unitId, String key);

    /**
     * Valeur convertie selon le type de l'attribut : {@code String}, {@code Long}, {@code BigDecimal},
     * {@code Boolean} ou {@code LocalDate}.
     */
    Optional<Object> getTypedValue(String unitId, String key);

    /** Définit ou modifie la valeur d'un attribut d'une unité, après validation. */
    AttributeValue setValue(String unitId, String key, String value);

    /** Supprime une valeur ; refusé si l'attribut est obligatoire. */
    void removeValue(String unitId, String key);

    // ------------------------------------------------------------ vérifications sans effet de bord

    /** Vérifie qu'une valeur est valide pour la définition {@code key} du type, sans rien enregistrer. */
    ValidationReport validateValue(String typeCode, String key, String value);

    /**
     * Vérifie un jeu de valeurs complet pour un type (attributs inconnus, valeurs invalides, obligatoires
     * manquants), typiquement avant de soumettre un formulaire de création d'unité.
     */
    ValidationReport validateValues(String typeCode, Map<String, String> values);
}
