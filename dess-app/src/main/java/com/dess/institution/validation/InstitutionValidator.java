package com.dess.institution.validation;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.Texts;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validateur métier (niveau 3) d'une configuration complète. Il est utilisé à la fois au chargement
 * du XML et après chaque modification : les règles ne sont écrites qu'à un seul endroit.
 */
public final class InstitutionValidator {

    /** Code de type ou d'unité. Doit rester cohérent avec {@code institution.xsd}. */
    public static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.\\-]*");

    private InstitutionValidator() {
    }

    /** Retourne toutes les violations (liste vide si la configuration est valide). */
    public static List<String> validate(InstitutionConfig config) {
        List<String> errors = new ArrayList<>();

        if (Texts.isBlank(config.id())) {
            errors.add("Établissement : identifiant obligatoire");
        }
        if (!CODE_PATTERN.matcher(config.code()).matches()) {
            errors.add("Établissement : code invalide");
        }
        if (Texts.isBlank(config.name())) {
            errors.add("Établissement : nom obligatoire");
        }

        validateTypes(config, errors);
        validatePairs(config, errors);
        HierarchyValidator.validate(config, errors);

        for (OrganizationalUnit u : config.units()) {
            config.findType(u.typeCode()).ifPresent(type ->
                    AttributeValidator.validateValues(HierarchyValidator.label(u), type, u.values(), errors));
        }
        return errors;
    }

    /** @throws ValidationException avec toutes les violations si la configuration est invalide */
    public static void validateOrThrow(InstitutionConfig config) {
        List<String> errors = validate(config);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateTypes(InstitutionConfig config, List<String> errors) {
        Set<String> codes = new HashSet<>();
        for (UnitTypeDefinition type : config.unitTypes()) {
            if (!CODE_PATTERN.matcher(type.code()).matches()) {
                errors.add("Type « " + type.code() + " » : code invalide");
            } else if (!codes.add(type.code())) {
                errors.add("Type « " + type.code() + " » : code de type dupliqué");
            }
            if (Texts.isBlank(type.label())) {
                errors.add("Type « " + type.code() + " » : libellé obligatoire");
            }
            AttributeValidator.validateDefinitions(type, errors);
        }
    }

    private static void validatePairs(InstitutionConfig config, List<String> errors) {
        Set<AllowedParentChildPair> seen = new HashSet<>();
        for (AllowedParentChildPair pair : config.allowedPairs()) {
            if (config.findType(pair.parent()).isEmpty()) {
                errors.add("Relation autorisée " + pair.parent() + " → " + pair.child()
                        + " : le type parent « " + pair.parent() + " » n'existe pas");
            }
            if (config.findType(pair.child()).isEmpty()) {
                errors.add("Relation autorisée " + pair.parent() + " → " + pair.child()
                        + " : le type enfant « " + pair.child() + " » n'existe pas");
            }
            if (!seen.add(pair)) {
                errors.add("Relation autorisée " + pair.parent() + " → " + pair.child() + " : dupliquée");
            }
        }
    }
}
