package com.dess.institution.validation;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.Texts;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @brief Business validator for a complete institution configuration.
 *
 * <p>The validator checks the general institution information, unit types, allowed parent-child
 * relations, the organizational tree and attribute values. It is used both when loading XML and
 * after every modification, so the rules are defined in a single place.</p>
 */
public final class InstitutionValidator
{
    /**
     * Code pattern for unit types and organizational units. It must stay consistent with
     * {@code institution.xsd}.
     */
    public static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.\\-]*");

    private InstitutionValidator()
    {
    }

    /**
     * Validates a complete configuration.
     *
     * @param config configuration to validate
     * @return all detected violations; the list is empty when the configuration is valid
     */
    public static List<String> validate(InstitutionConfig config)
    {
        List<String> errors = new ArrayList<>();

        if (Texts.isBlank(config.id()))
        {
            errors.add("Institution: identifier required");
        }

        if (!CODE_PATTERN.matcher(config.code()).matches())
        {
            errors.add("Institution: invalid code");
        }

        if (Texts.isBlank(config.name()))
        {
            errors.add("Institution: name required");
        }

        validateTypes(config, errors);
        validatePairs(config, errors);
        validateHierarchy(config, errors);

        for (OrganizationalUnit unit : config.units())
        {
            config.findType(unit.typeCode())
                  .ifPresent(type -> AttributeValidator.validateValues(
                      label(unit),
                      type,
                      unit.values(),
                      errors
                  ));
        }

        return errors;
    }

    /**
     * Validates a complete configuration and throws when it is invalid.
     *
     * @param config configuration to validate
     * @throws ValidationException containing every detected violation
     */
    public static void validateOrThrow(InstitutionConfig config)
    {
        List<String> errors = validate(config);

        if (!errors.isEmpty())
        {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates unit type codes, labels, uniqueness and attribute definitions.
     *
     * @param config configuration being validated
     * @param errors list receiving the detected violations
     */
    private static void validateTypes(InstitutionConfig config, List<String> errors)
    {
        Set<String> codes = new HashSet<>();

        for (UnitTypeDefinition type : config.unitTypes())
        {
            if (!CODE_PATTERN.matcher(type.code()).matches())
            {
                errors.add("Type '" + type.code() + "': invalid code");
            }
            else if (!codes.add(type.code()))
            {
                errors.add("Type '" + type.code() + "': duplicate type code");
            }

            if (Texts.isBlank(type.label()))
            {
                errors.add("Type '" + type.code() + "': label required");
            }

            AttributeValidator.validateDefinitions(type, errors);
        }
    }

    /**
     * Validates allowed parent-child relations: referenced types must exist and relations must be
     * unique.
     *
     * @param config configuration being validated
     * @param errors list receiving the detected violations
     */
    private static void validatePairs(InstitutionConfig config, List<String> errors)
    {
        Set<AllowedParentChildPair> seen = new HashSet<>();

        for (AllowedParentChildPair pair : config.allowedPairs())
        {
            if (config.findType(pair.parent()).isEmpty())
            {
                errors.add("Allowed relation " + pair.parent() + " → " + pair.child()
                           + ": parent type '" + pair.parent() + "' does not exist");
            }

            if (config.findType(pair.child()).isEmpty())
            {
                errors.add("Allowed relation " + pair.parent() + " → " + pair.child()
                           + ": child type '" + pair.child() + "' does not exist");
            }

            if (!seen.add(pair))
            {
                errors.add("Allowed relation " + pair.parent() + " → " + pair.child() + ": duplicate");
            }
        }
    }

    /**
     * Validates the structural rules of the organizational tree.
     *
     * <p>The checks cover unique identifiers and codes, existing types and parents, allowed
     * parent-child relations, absence of cycles, and active/inactive consistency.</p>
     *
     * @param config configuration being validated
     * @param errors list receiving the detected violations
     */
    private static void validateHierarchy(InstitutionConfig config, List<String> errors)
    {
        Map<String, OrganizationalUnit> unitsById = new HashMap<>();
        Set<String> codes = new HashSet<>();

        for (OrganizationalUnit unit : config.units())
        {
            String label = label(unit);

            if (Texts.isBlank(unit.id()))
            {
                errors.add("Unit " + label + ": identifier required");
            }
            else if (unitsById.putIfAbsent(unit.id(), unit) != null)
            {
                errors.add("Unit " + label + ": duplicate identifier '" + unit.id() + "'");
            }

            if (!CODE_PATTERN.matcher(unit.code()).matches())
            {
                errors.add("Unit " + label + ": invalid code");
            }
            else if (!codes.add(unit.code()))
            {
                errors.add("Unit " + label + ": duplicate code '" + unit.code() + "'");
            }

            if (Texts.isBlank(unit.name()))
            {
                errors.add("Unit " + label + ": name required");
            }
        }

        for (OrganizationalUnit unit : config.units())
        {
            String label = label(unit);
            var type = config.findType(unit.typeCode());

            if (type.isEmpty())
            {
                errors.add("Unit " + label + ": type '" + unit.typeCode() + "' does not exist");
            }

            if (unit.parentId() == null)
            {
                continue;
            }

            if (unit.parentId().equals(unit.id()))
            {
                errors.add("Unit " + label + ": a unit cannot be its own parent");
                continue;
            }

            OrganizationalUnit parent = unitsById.get(unit.parentId());

            if (parent == null)
            {
                errors.add("Unit " + label + ": parent '" + unit.parentId() + "' does not exist");
                continue;
            }

            if (type.isPresent()
                && config.findType(parent.typeCode()).isPresent()
                && !config.isAllowed(parent.typeCode(), unit.typeCode()))
            {
                errors.add("Unit " + label + ": forbidden relation, a unit of type '" + unit.typeCode()
                           + "' cannot have a unit of type '" + parent.typeCode() + "' as parent");
            }

            if (unit.active() && !parent.active())
            {
                errors.add("Unit " + label + ": is active while its parent " + label(parent)
                           + " is inactive");
            }
        }

        detectCycles(unitsById, errors);
    }

    /**
     * Detects parent cycles and reports each cycle once.
     *
     * @param unitsById units indexed by identifier
     * @param errors    list receiving the detected violations
     */
    private static void detectCycles(Map<String, OrganizationalUnit> unitsById, List<String> errors)
    {
        Set<String> reported = new HashSet<>();

        for (OrganizationalUnit start : unitsById.values())
        {
            Set<String> visited = new HashSet<>();
            OrganizationalUnit current = start;

            while (current != null
                   && current.parentId() != null
                   && !current.parentId().equals(current.id()))
            {
                if (!visited.add(current.id()))
                {
                    if (reported.add(current.id()))
                    {
                        errors.add("Cycle detected in the hierarchy at unit " + label(current));
                    }

                    reported.addAll(visited);
                    break;
                }

                current = unitsById.get(current.parentId());
            }
        }
    }

    private static String label(OrganizationalUnit unit)
    {
        return "'" + unit.name() + "' (" + unit.code() + ")";
    }
}
