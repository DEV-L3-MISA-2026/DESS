package com.dess.institution.application.impl;

import com.dess.institution.application.UnitTypeService;
import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UnitTypeServiceImpl implements UnitTypeService {

    private final ConfigurationHolder holder;

    public UnitTypeServiceImpl(ConfigurationHolder holder) {
        this.holder = holder;
    }

    @Override
    public UnitTypeDefinition create(UnitTypeDefinition type) {
        return holder.update(cfg -> {
            if (cfg.findType(type.code()).isPresent()) {
                throw new ValidationException("Le type « " + type.code() + " » existe déjà");
            }
            List<UnitTypeDefinition> types = new ArrayList<>(cfg.unitTypes());
            types.add(type);
            return new ConfigurationHolder.Change<>(cfg.withUnitTypes(types), type);
        });
    }

    @Override
    public UnitTypeDefinition update(String code, String label, String description) {
        return holder.update(cfg -> {
            UnitTypeDefinition updated = require(cfg, code).withLabelAndDescription(label, description);
            return new ConfigurationHolder.Change<>(cfg.replaceType(updated), updated);
        });
    }

    @Override
    public Optional<UnitTypeDefinition> getByCode(String code) {
        return holder.current().findType(code);
    }

    @Override
    public List<UnitTypeDefinition> getAll() {
        return holder.current().unitTypes();
    }

    @Override
    public UnitTypeDefinition deactivate(String code) {
        return setActive(code, false);
    }

    @Override
    public UnitTypeDefinition activate(String code) {
        return setActive(code, true);
    }

    private UnitTypeDefinition setActive(String code, boolean active) {
        return holder.update(cfg -> {
            UnitTypeDefinition updated = require(cfg, code).withActive(active);
            return new ConfigurationHolder.Change<>(cfg.replaceType(updated), updated);
        });
    }

    @Override
    public void delete(String code) {
        holder.update(cfg -> {
            require(cfg, code);
            long usedBy = cfg.units().stream().filter(u -> u.typeCode().equals(code)).count();
            if (usedBy > 0) {
                throw new ValidationException("Le type « " + code + " » est utilisé par " + usedBy
                        + " unité(s) : suppression refusée");
            }
            List<String> pairs = cfg.allowedPairs().stream()
                    .filter(p -> p.parent().equals(code) || p.child().equals(code))
                    .map(p -> p.parent() + " → " + p.child())
                    .toList();
            if (!pairs.isEmpty()) {
                throw new ValidationException("Le type « " + code + " » est référencé par des relations autorisées ("
                        + String.join(", ", pairs) + ") : les retirer d'abord");
            }
            List<UnitTypeDefinition> types = new ArrayList<>(cfg.unitTypes());
            types.removeIf(t -> t.code().equals(code));
            return new ConfigurationHolder.Change<>(cfg.withUnitTypes(types), null);
        });
    }

    // ------------------------------------------------------------ relations autorisées

    @Override
    public List<AllowedParentChildPair> getAllowedPairs() {
        return holder.current().allowedPairs();
    }

    @Override
    public AllowedParentChildPair addAllowedPair(String parentType, String childType) {
        return holder.update(cfg -> {
            AllowedParentChildPair pair = new AllowedParentChildPair(parentType, childType);
            List<AllowedParentChildPair> pairs = new ArrayList<>(cfg.allowedPairs());
            pairs.add(pair);
            return new ConfigurationHolder.Change<>(cfg.withAllowedPairs(pairs), pair);
        });
    }

    @Override
    public void removeAllowedPair(String parentType, String childType) {
        holder.update(cfg -> {
            AllowedParentChildPair pair = new AllowedParentChildPair(parentType, childType);
            if (!cfg.allowedPairs().contains(pair)) {
                throw new NotFoundException("Relation autorisée introuvable : " + parentType + " → " + childType);
            }
            List<AllowedParentChildPair> pairs = new ArrayList<>(cfg.allowedPairs());
            pairs.remove(pair);
            return new ConfigurationHolder.Change<>(cfg.withAllowedPairs(pairs), null);
        });
    }

    @Override
    public boolean isAllowed(String parentType, String childType) {
        return holder.current().isAllowed(parentType, childType);
    }

    @Override
    public List<String> getAllowedChildTypes(String parentType) {
        return holder.current().allowedPairs().stream()
                .filter(p -> p.parent().equals(parentType))
                .map(AllowedParentChildPair::child)
                .toList();
    }

    @Override
    public List<String> getAllowedParentTypes(String childType) {
        return holder.current().allowedPairs().stream()
                .filter(p -> p.child().equals(childType))
                .map(AllowedParentChildPair::parent)
                .toList();
    }

    private static UnitTypeDefinition require(InstitutionConfig cfg, String code) {
        return cfg.findType(code).orElseThrow(() -> new NotFoundException("Type d'unité introuvable : " + code));
    }
}
