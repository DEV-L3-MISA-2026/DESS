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
import com.dess.institution.validation.AttributeValueConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AttributeServiceImpl implements AttributeService {

    private final ConfigurationHolder holder;

    public AttributeServiceImpl(ConfigurationHolder holder) {
        this.holder = holder;
    }

    // ------------------------------------------------------------ définitions

    @Override
    public List<AttributeDefinition> getDefinitions(String typeCode) {
        return requireType(holder.current(), typeCode).attributes();
    }

    @Override
    public AttributeDefinition addDefinition(String typeCode, AttributeDefinition definition) {
        return holder.update(cfg -> {
            UnitTypeDefinition type = requireType(cfg, typeCode);
            if (type.findAttribute(definition.key()).isPresent()) {
                throw new ValidationException("Le type « " + typeCode + " » possède déjà l'attribut « "
                        + definition.key() + " »");
            }
            List<AttributeDefinition> defs = new ArrayList<>(type.attributes());
            defs.add(definition);
            UnitTypeDefinition newType = type.withAttributes(defs);
            InstitutionConfig next = backfillDefault(cfg.replaceType(newType), typeCode, definition);
            return new ConfigurationHolder.Change<>(next, definition);
        });
    }

    @Override
    public AttributeDefinition updateDefinition(String typeCode, AttributeDefinition definition) {
        return holder.update(cfg -> {
            UnitTypeDefinition type = requireType(cfg, typeCode);
            if (type.findAttribute(definition.key()).isEmpty()) {
                throw new NotFoundException("Attribut « " + definition.key() + " » introuvable dans le type « "
                        + typeCode + " »");
            }
            List<AttributeDefinition> defs = new ArrayList<>();
            for (AttributeDefinition d : type.attributes()) {
                defs.add(d.key().equals(definition.key()) ? definition : d);
            }
            InstitutionConfig next = backfillDefault(cfg.replaceType(type.withAttributes(defs)), typeCode, definition);
            return new ConfigurationHolder.Change<>(next, definition);
        });
    }

    @Override
    public void removeDefinition(String typeCode, String key) {
        holder.update(cfg -> {
            UnitTypeDefinition type = requireType(cfg, typeCode);
            if (type.findAttribute(key).isEmpty()) {
                throw new NotFoundException("Attribut « " + key + " » introuvable dans le type « " + typeCode + " »");
            }
            long holders = cfg.units().stream()
                    .filter(u -> u.typeCode().equals(typeCode) && u.values().containsKey(key))
                    .count();
            if (holders > 0) {
                throw new ValidationException("L'attribut « " + key + " » porte une valeur dans " + holders
                        + " unité(s) : supprimer d'abord ces valeurs");
            }
            List<AttributeDefinition> defs = new ArrayList<>(type.attributes());
            defs.removeIf(d -> d.key().equals(key));
            return new ConfigurationHolder.Change<>(cfg.replaceType(type.withAttributes(defs)), null);
        });
    }

    /**
     * Si l'attribut est obligatoire et possède une valeur par défaut, les unités existantes du type qui
     * n'ont pas de valeur reçoivent cette valeur : la configuration reste valide sans intervention.
     */
    private static InstitutionConfig backfillDefault(InstitutionConfig cfg, String typeCode, AttributeDefinition def) {
        if (!def.required() || !def.hasDefault()) {
            return cfg;
        }
        List<OrganizationalUnit> units = new ArrayList<>();
        for (OrganizationalUnit u : cfg.units()) {
            if (u.typeCode().equals(typeCode)) {
                AttributeValue existing = u.values().get(def.key());
                if (existing == null || existing.value().isBlank()) {
                    Map<String, AttributeValue> values = new LinkedHashMap<>(u.values());
                    values.put(def.key(), new AttributeValue(def.key(), def.defaultValue()));
                    u = u.withValues(values);
                }
            }
            units.add(u);
        }
        return cfg.withUnits(units);
    }

    // ------------------------------------------------------------ valeurs

    @Override
    public List<AttributeValue> getValues(String unitId) {
        return List.copyOf(requireUnit(holder.current(), unitId).values().values());
    }

    @Override
    public Optional<String> getValue(String unitId, String key) {
        return requireUnit(holder.current(), unitId).value(key).map(AttributeValue::value);
    }

    @Override
    public Optional<Object> getTypedValue(String unitId, String key) {
        InstitutionConfig cfg = holder.current();
        OrganizationalUnit unit = requireUnit(cfg, unitId);
        Optional<AttributeValue> value = unit.value(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        AttributeDefinition def = requireType(cfg, unit.typeCode()).findAttribute(key).orElseThrow(() ->
                new NotFoundException("Attribut « " + key + " » non défini pour le type « " + unit.typeCode() + " »"));
        return Optional.of(AttributeValueConverter.convert(def.type(), value.get().value()));
    }

    @Override
    public AttributeValue setValue(String unitId, String key, String value) {
        return holder.update(cfg -> {
            OrganizationalUnit unit = requireUnit(cfg, unitId);
            UnitTypeDefinition type = requireType(cfg, unit.typeCode());
            if (type.findAttribute(key).isEmpty()) {
                throw new ValidationException("L'attribut « " + key + " » n'est pas défini pour le type « "
                        + type.code() + " »");
            }
            if (Texts.isBlank(value)) {
                throw new ValidationException("Valeur vide pour l'attribut « " + key
                        + " » : utiliser removeValue pour supprimer une valeur");
            }
            AttributeValue newValue = new AttributeValue(key, value.trim());
            Map<String, AttributeValue> values = new LinkedHashMap<>(unit.values());
            values.put(key, newValue);
            return new ConfigurationHolder.Change<>(cfg.replaceUnit(unit.withValues(values)), newValue);
        });
    }

    @Override
    public void removeValue(String unitId, String key) {
        holder.update(cfg -> {
            OrganizationalUnit unit = requireUnit(cfg, unitId);
            if (!unit.values().containsKey(key)) {
                throw new NotFoundException("Aucune valeur « " + key + " » pour l'unité « " + unit.name() + " »");
            }
            Map<String, AttributeValue> values = new LinkedHashMap<>(unit.values());
            values.remove(key);
            return new ConfigurationHolder.Change<>(cfg.replaceUnit(unit.withValues(values)), null);
        });
    }

    // ------------------------------------------------------------ vérifications

    @Override
    public ValidationReport validateValue(String typeCode, String key, String value) {
        UnitTypeDefinition type = requireType(holder.current(), typeCode);
        Optional<AttributeDefinition> def = type.findAttribute(key);
        if (def.isEmpty()) {
            return new ValidationReport(List.of("L'attribut « " + key + " » n'est pas défini pour le type « "
                    + typeCode + " »"));
        }
        return new ValidationReport(AttributeValueConverter.check(def.get().type(), value)
                .map(msg -> List.of("Attribut « " + key + " » : " + msg))
                .orElse(List.of()));
    }

    @Override
    public ValidationReport validateValues(String typeCode, Map<String, String> values) {
        UnitTypeDefinition type = requireType(holder.current(), typeCode);
        List<String> errors = new ArrayList<>();
        AttributeValidator.validateValues("(saisie)", type, UnitValues.build(type, values), errors);
        return new ValidationReport(errors);
    }

    private static UnitTypeDefinition requireType(InstitutionConfig cfg, String code) {
        return cfg.findType(code).orElseThrow(() -> new NotFoundException("Type d'unité introuvable : " + code));
    }

    private static OrganizationalUnit requireUnit(InstitutionConfig cfg, String id) {
        return cfg.findUnit(id).orElseThrow(() -> new NotFoundException("Unité introuvable : " + id));
    }
}
