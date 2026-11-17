package com.dess.institution.validation;

import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.Texts;
import com.dess.institution.domain.UnitTypeDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Règles de validation des définitions d'attributs et des valeurs des unités. */
public final class AttributeValidator {

    /** Clé d'attribut : commence par une lettre, puis lettres, chiffres ou {@code _}. */
    public static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private AttributeValidator() {
    }

    /** Valide les définitions d'un type : clés bien formées et uniques, libellés, valeurs par défaut. */
    public static void validateDefinitions(UnitTypeDefinition type, List<String> errors) {
        Set<String> keys = new HashSet<>();
        for (AttributeDefinition def : type.attributes()) {
            String where = "Type « " + type.code() + " », attribut « " + def.key() + " » : ";
            if (!KEY_PATTERN.matcher(def.key()).matches()) {
                errors.add(where + "clé invalide (lettre puis lettres, chiffres ou _)");
            } else if (!keys.add(def.key())) {
                errors.add(where + "clé dupliquée dans le type");
            }
            if (Texts.isBlank(def.label())) {
                errors.add(where + "libellé obligatoire");
            }
            if (def.hasDefault()) {
                AttributeValueConverter.check(def.type(), def.defaultValue())
                        .ifPresent(msg -> errors.add(where + "valeur par défaut invalide : " + msg));
            }
        }
    }

    /**
     * Valide les valeurs d'une unité par rapport aux définitions de son type : clés connues,
     * valeurs conformes au type de donnée, attributs obligatoires présents et non vides.
     *
     * @param unitLabel libellé de l'unité pour les messages (ex. « Mention Droit (MENTION_DROIT) »)
     */
    public static void validateValues(String unitLabel, UnitTypeDefinition type,
                                      Map<String, AttributeValue> values, List<String> errors) {
        for (AttributeValue value : values.values()) {
            Optional<AttributeDefinition> def = type.findAttribute(value.key());
            if (def.isEmpty()) {
                errors.add("Unité " + unitLabel + " : l'attribut « " + value.key()
                        + " » n'est pas défini pour le type « " + type.code() + " »");
                continue;
            }
            AttributeValueConverter.check(def.get().type(), value.value())
                    .ifPresent(msg -> errors.add("Unité " + unitLabel + ", attribut « " + value.key() + " » : " + msg));
        }
        for (AttributeDefinition def : type.attributes()) {
            if (def.required()) {
                AttributeValue value = values.get(def.key());
                if (value == null || value.value().isBlank()) {
                    errors.add("Unité " + unitLabel + " : l'attribut obligatoire « " + def.key() + " » est manquant");
                }
            }
        }
    }
}
