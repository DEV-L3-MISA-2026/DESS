package com.dess.institution.application.impl;

import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.UnitTypeDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/** Normalisation des valeurs saisies avant validation : suppression des blancs vides, valeurs par défaut. */
final class UnitValues {

    private UnitValues() {
    }

    /**
     * Construit la map de valeurs d'une unité à partir de la saisie : les valeurs blanches sont ignorées
     * (une valeur absente), les valeurs par défaut sont appliquées aux attributs non renseignés. Les clés
     * inconnues sont conservées telles quelles afin que le validateur les signale.
     */
    static Map<String, AttributeValue> build(UnitTypeDefinition type, Map<String, String> input) {
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        if (input != null) {
            input.forEach((key, raw) -> {
                if (raw != null && !raw.isBlank()) {
                    values.put(key, new AttributeValue(key, raw.trim()));
                }
            });
        }
        applyDefaults(type, values);
        return values;
    }

    static void applyDefaults(UnitTypeDefinition type, Map<String, AttributeValue> values) {
        for (AttributeDefinition def : type.attributes()) {
            if (def.hasDefault() && !values.containsKey(def.key())) {
                values.put(def.key(), new AttributeValue(def.key(), def.defaultValue()));
            }
        }
    }
}
