package com.dess.institution.validation;

import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.Texts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Règles structurelles de l'arbre : identifiants et codes uniques, types et parents existants,
 * relations parent-enfant autorisées, absence de cycles, cohérence actif/inactif.
 */
public final class HierarchyValidator {

    private HierarchyValidator() {
    }

    public static void validate(InstitutionConfig config, List<String> errors) {
        Map<String, OrganizationalUnit> byId = new HashMap<>();
        Set<String> codes = new HashSet<>();

        for (OrganizationalUnit u : config.units()) {
            String label = label(u);
            if (Texts.isBlank(u.id())) {
                errors.add("Unité " + label + " : identifiant obligatoire");
            } else if (byId.putIfAbsent(u.id(), u) != null) {
                errors.add("Unité " + label + " : identifiant « " + u.id() + " » dupliqué");
            }
            if (!InstitutionValidator.CODE_PATTERN.matcher(u.code()).matches()) {
                errors.add("Unité " + label + " : code invalide");
            } else if (!codes.add(u.code())) {
                errors.add("Unité " + label + " : code « " + u.code() + " » dupliqué");
            }
            if (Texts.isBlank(u.name())) {
                errors.add("Unité " + label + " : nom obligatoire");
            }
        }

        for (OrganizationalUnit u : config.units()) {
            String label = label(u);
            var type = config.findType(u.typeCode());
            if (type.isEmpty()) {
                errors.add("Unité " + label + " : le type « " + u.typeCode() + " » n'existe pas");
            }
            if (u.parentId() == null) {
                continue;
            }
            if (u.parentId().equals(u.id())) {
                errors.add("Unité " + label + " : une unité ne peut pas être son propre parent");
                continue;
            }
            OrganizationalUnit parent = byId.get(u.parentId());
            if (parent == null) {
                errors.add("Unité " + label + " : le parent « " + u.parentId() + " » n'existe pas");
                continue;
            }
            if (type.isPresent() && config.findType(parent.typeCode()).isPresent()
                    && !config.isAllowed(parent.typeCode(), u.typeCode())) {
                errors.add("Unité " + label + " : relation interdite, une unité de type « " + u.typeCode()
                        + " » ne peut pas avoir pour parent une unité de type « " + parent.typeCode() + " »");
            }
            if (u.active() && !parent.active()) {
                errors.add("Unité " + label + " : est active alors que son parent " + label(parent) + " est inactif");
            }
        }

        detectCycles(byId, errors);
    }

    private static void detectCycles(Map<String, OrganizationalUnit> byId, List<String> errors) {
        Set<String> reported = new HashSet<>();
        for (OrganizationalUnit start : byId.values()) {
            Set<String> seen = new HashSet<>();
            OrganizationalUnit current = start;
            while (current != null && current.parentId() != null && !current.parentId().equals(current.id())) {
                if (!seen.add(current.id())) {
                    if (reported.add(current.id())) {
                        errors.add("Cycle détecté dans la hiérarchie au niveau de l'unité " + label(current));
                    }
                    reported.addAll(seen);
                    break;
                }
                current = byId.get(current.parentId());
            }
        }
    }

    static String label(OrganizationalUnit u) {
        return "« " + u.name() + " » (" + u.code() + ")";
    }
}
