package com.dess.institution.application.impl;

import com.dess.institution.application.AttributeService;
import com.dess.institution.application.ValidationReport;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.Texts;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;
import com.dess.institution.validation.AttributeValidator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @brief Default implementation of {@link AttributeService}.
 *
 * <p>The service works on immutable configuration snapshots obtained from
 * {@link ConfigurationHolder}. Every write is executed inside
 * {@link ConfigurationHolder#update(java.util.function.Function)}, which validates and persists the
 * resulting configuration before publishing it.</p>
 */
public final class AttributeServiceImpl implements AttributeService
{
    private final ConfigurationHolder holder;

    /**
     * Creates the service.
     *
     * @param holder configuration holder shared by the module
     * @throws NullPointerException if {@code holder} is {@code null}
     */
    public AttributeServiceImpl(ConfigurationHolder holder)
    {
        this.holder = Objects.requireNonNull(holder, "holder");
    }

    @Override
    public List<AttributeDefinition> getDefinitions(String typeCode)
    {
        return requireType(holder.current(), typeCode).attributes();
    }

    @Override
    public AttributeDefinition addDefinition(String typeCode, AttributeDefinition definition)
    {
        return holder.update(configuration ->
        {
            UnitTypeDefinition type = requireType(configuration, typeCode);

            if (type.findAttribute(definition.key()).isPresent())
            {
                throw new ValidationException("Unit type '" + typeCode + "' already has attribute '"
                                              + definition.key() + "'");
            }

            List<AttributeDefinition> definitions = new ArrayList<>(type.attributes());
            definitions.add(definition);
            UnitTypeDefinition updatedType = type.withAttributes(definitions);
            InstitutionConfig next = backfillDefault(
                configuration.replaceType(updatedType),
                typeCode,
                definition
            );

            return new ConfigurationHolder.Change<>(next, definition);
        });
    }

    @Override
    public AttributeDefinition updateDefinition(String typeCode, AttributeDefinition definition)
    {
        return holder.update(configuration ->
        {
            UnitTypeDefinition type = requireType(configuration, typeCode);

            if (type.findAttribute(definition.key()).isEmpty())
            {
                throw new NotFoundException("Attribute '" + definition.key() + "' not found in type '"
                                            + typeCode + "'");
            }

            List<AttributeDefinition> definitions = new ArrayList<>();

            for (AttributeDefinition current : type.attributes())
            {
                definitions.add(current.key().equals(definition.key()) ? definition : current);
            }

            UnitTypeDefinition updatedType = type.withAttributes(definitions);
            InstitutionConfig next = backfillDefault(
                configuration.replaceType(updatedType),
                typeCode,
                definition
            );

            return new ConfigurationHolder.Change<>(next, definition);
        });
    }

    @Override
    public void removeDefinition(String typeCode, String key)
    {
        holder.update(configuration ->
        {
            UnitTypeDefinition type = requireType(configuration, typeCode);

            if (type.findAttribute(key).isEmpty())
            {
                throw new NotFoundException("Attribute '" + key + "' not found in type '" + typeCode + "'");
            }

            long holders = configuration.units()
                                        .stream()
                                        .filter(unit -> unit.typeCode().equals(typeCode)
                                                        && unit.values().containsKey(key))
                                        .count();

            if (holders > 0)
            {
                throw new ValidationException("Attribute '" + key + "' holds a value in " + holders
                                              + " unit(s): delete those values first");
            }

            List<AttributeDefinition> definitions = new ArrayList<>(type.attributes());
            definitions.removeIf(current -> current.key().equals(key));

            UnitTypeDefinition updatedType = type.withAttributes(definitions);
            return new ConfigurationHolder.Change<>(
                configuration.replaceType(updatedType),
                null
            );
        });
    }

    @Override
    public List<AttributeValue> getValues(String unitId)
    {
        return List.copyOf(requireUnit(holder.current(), unitId).values().values());
    }

    @Override
    public Optional<String> getValue(String unitId, String key)
    {
        return requireUnit(holder.current(), unitId)
            .value(key)
            .map(AttributeValue::value);
    }

    @Override
    public Optional<Object> getTypedValue(String unitId, String key)
    {
        InstitutionConfig configuration = holder.current();
        OrganizationalUnit unit = requireUnit(configuration, unitId);
        Optional<AttributeValue> value = unit.value(key);

        if (value.isEmpty())
        {
            return Optional.empty();
        }

        AttributeDefinition definition = requireType(configuration, unit.typeCode())
            .findAttribute(key)
            .orElseThrow(() -> new NotFoundException(
                "Attribute '" + key + "' not defined for type '" + unit.typeCode() + "'"
            ));

        return Optional.of(definition.type().convert(value.get().value()));
    }

    @Override
    public AttributeValue setValue(String unitId, String key, String value)
    {
        return holder.update(configuration ->
        {
            OrganizationalUnit unit = requireUnit(configuration, unitId);
            UnitTypeDefinition type = requireType(configuration, unit.typeCode());

            if (type.findAttribute(key).isEmpty())
            {
                throw new ValidationException("Attribute '" + key + "' is not defined for type '"
                                              + type.code() + "'");
            }

            if (Texts.isBlank(value))
            {
                throw new ValidationException("Empty value for attribute '" + key
                                              + "': use removeValue to delete a value");
            }

            AttributeValue newValue = new AttributeValue(key, value.strip());
            Map<String, AttributeValue> values = new LinkedHashMap<>(unit.values());
            values.put(key, newValue);

            return new ConfigurationHolder.Change<>(
                configuration.replaceUnit(unit.withValues(values)),
                newValue
            );
        });
    }

    @Override
    public void removeValue(String unitId, String key)
    {
        holder.update(configuration ->
        {
            OrganizationalUnit unit = requireUnit(configuration, unitId);

            if (!unit.values().containsKey(key))
            {
                throw new NotFoundException("No value '" + key + "' for unit '" + unit.name() + "'");
            }

            Map<String, AttributeValue> values = new LinkedHashMap<>(unit.values());
            values.remove(key);

            return new ConfigurationHolder.Change<>(
                configuration.replaceUnit(unit.withValues(values)),
                null
            );
        });
    }

    @Override
    public ValidationReport validateValue(String typeCode, String key, String value)
    {
        UnitTypeDefinition type = requireType(holder.current(), typeCode);
        Optional<AttributeDefinition> definition = type.findAttribute(key);

        if (definition.isEmpty())
        {
            return new ValidationReport(List.of(
                "Attribute '" + key + "' is not defined for type '" + typeCode + "'"
            ));
        }

        List<String> errors = definition.get()
                                        .type()
                                        .check(value)
                                        .map(message -> List.of("Attribute '" + key + "': " + message))
                                        .orElse(List.of());

        return new ValidationReport(errors);
    }

    @Override
    public ValidationReport validateValues(String typeCode, Map<String, String> values)
    {
        UnitTypeDefinition type = requireType(holder.current(), typeCode);
        List<String> errors = new ArrayList<>();

        AttributeValidator.validateValues("(input)", type, UnitValues.build(type, values), errors);

        return new ValidationReport(errors);
    }

    /**
     * Applies the default value of a required attribute to existing units that do not define it.
     *
     * @param configuration configuration to complete
     * @param typeCode      code of the unit type receiving the attribute
     * @param definition    attribute definition being added or updated
     * @return the completed configuration, or the original one when no backfill is needed
     */
    private static InstitutionConfig backfillDefault(InstitutionConfig configuration, String typeCode,
                                                     AttributeDefinition definition)
    {
        if (!definition.required() || !definition.hasDefault())
        {
            return configuration;
        }

        List<OrganizationalUnit> units = new ArrayList<>();

        for (OrganizationalUnit unit : configuration.units())
        {
            if (unit.typeCode().equals(typeCode))
            {
                AttributeValue existing = unit.values().get(definition.key());

                if (existing == null || existing.value().isBlank())
                {
                    Map<String, AttributeValue> values = new LinkedHashMap<>(unit.values());
                    values.put(definition.key(), new AttributeValue(definition.key(), definition.defaultValue()));
                    unit = unit.withValues(values);
                }
            }

            units.add(unit);
        }

        return configuration.withUnits(units);
    }

    private static UnitTypeDefinition requireType(InstitutionConfig configuration, String code)
    {
        return configuration.findType(code)
                            .orElseThrow(() -> new NotFoundException("Unit type not found: " + code));
    }

    private static OrganizationalUnit requireUnit(InstitutionConfig configuration, String id)
    {
        return configuration.findUnit(id)
                            .orElseThrow(() -> new NotFoundException("Unit not found: " + id));
    }
}
