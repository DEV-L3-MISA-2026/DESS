package com.dess.institution.application.impl;

import com.dess.institution.application.CreateUnitCommand;
import com.dess.institution.application.HierarchyService;
import com.dess.institution.application.UpdateUnitCommand;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.Texts;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class HierarchyServiceImpl implements HierarchyService {

    private final ConfigurationHolder holder;

    public HierarchyServiceImpl(ConfigurationHolder holder) {
        this.holder = holder;
    }

    // ------------------------------------------------------------ consultation

    @Override
    public Optional<OrganizationalUnit> getById(String id) {
        return holder.current().findUnit(id);
    }

    @Override
    public Optional<OrganizationalUnit> getByCode(String code) {
        return holder.current().findUnitByCode(code);
    }

    @Override
    public List<OrganizationalUnit> getRoots() {
        return holder.current().units().stream().filter(OrganizationalUnit::isRoot).toList();
    }

    @Override
    public List<OrganizationalUnit> getChildren(String id) {
        InstitutionConfig cfg = holder.current();
        require(cfg, id);
        return cfg.childrenOf(id);
    }

    @Override
    public Optional<OrganizationalUnit> getParent(String id) {
        InstitutionConfig cfg = holder.current();
        OrganizationalUnit unit = require(cfg, id);
        return unit.parentId() == null ? Optional.empty() : cfg.findUnit(unit.parentId());
    }

    @Override
    public List<OrganizationalUnit> getDescendants(String id) {
        InstitutionConfig cfg = holder.current();
        require(cfg, id);
        return descendants(cfg, id);
    }

    @Override
    public List<OrganizationalUnit> getAncestors(String id) {
        InstitutionConfig cfg = holder.current();
        return ancestors(cfg, require(cfg, id));
    }

    @Override
    public List<OrganizationalUnit> getPath(String id) {
        InstitutionConfig cfg = holder.current();
        OrganizationalUnit unit = require(cfg, id);
        List<OrganizationalUnit> path = new ArrayList<>(ancestors(cfg, unit));
        Collections.reverse(path);
        path.add(unit);
        return List.copyOf(path);
    }

    @Override
    public List<OrganizationalUnit> getByType(String typeCode) {
        return holder.current().units().stream().filter(u -> u.typeCode().equals(typeCode)).toList();
    }

    @Override
    public List<OrganizationalUnit> getSubtree(String id) {
        InstitutionConfig cfg = holder.current();
        OrganizationalUnit unit = require(cfg, id);
        List<OrganizationalUnit> result = new ArrayList<>();
        result.add(unit);
        result.addAll(descendants(cfg, id));
        return List.copyOf(result);
    }

    @Override
    public boolean isDescendantOf(String candidateId, String ancestorId) {
        InstitutionConfig cfg = holder.current();
        OrganizationalUnit candidate = require(cfg, candidateId);
        require(cfg, ancestorId);
        return ancestors(cfg, candidate).stream().anyMatch(a -> a.id().equals(ancestorId));
    }

    @Override
    public boolean areInSameBranch(String idA, String idB) {
        return idA.equals(idB) ? require(holder.current(), idA) != null
                : isDescendantOf(idA, idB) || isDescendantOf(idB, idA);
    }

    @Override
    public Optional<OrganizationalUnit> getNearestCommonAncestor(String idA, String idB) {
        InstitutionConfig cfg = holder.current();
        OrganizationalUnit a = require(cfg, idA);
        OrganizationalUnit b = require(cfg, idB);

        Set<String> fromA = new HashSet<>();
        fromA.add(a.id());
        ancestors(cfg, a).forEach(u -> fromA.add(u.id()));

        List<OrganizationalUnit> chainB = new ArrayList<>();
        chainB.add(b);
        chainB.addAll(ancestors(cfg, b));
        return chainB.stream().filter(u -> fromA.contains(u.id())).findFirst();
    }

    // ------------------------------------------------------------ modification

    @Override
    public OrganizationalUnit createUnit(CreateUnitCommand cmd) {
        return holder.update(cfg -> {
            UnitTypeDefinition type = cfg.findType(Texts.orEmpty(cmd.typeCode())).orElseThrow(() ->
                    new ValidationException("Le type d'unité « " + cmd.typeCode() + " » n'existe pas"));
            if (!type.active()) {
                throw new ValidationException("Le type « " + type.code() + " » est inactif : "
                        + "impossible d'y créer de nouvelles unités");
            }
            String id = Texts.isBlank(cmd.id()) ? UUID.randomUUID().toString() : cmd.id();
            OrganizationalUnit unit = new OrganizationalUnit(id, cmd.code(), cmd.name(), cmd.description(),
                    type.code(), cmd.parentId(), cmd.active(), UnitValues.build(type, cmd.values()));
            List<OrganizationalUnit> units = new ArrayList<>(cfg.units());
            units.add(unit);
            return new ConfigurationHolder.Change<>(cfg.withUnits(units), unit);
        });
    }

    @Override
    public OrganizationalUnit updateUnit(String id, UpdateUnitCommand cmd) {
        return holder.update(cfg -> {
            OrganizationalUnit unit = require(cfg, id);
            OrganizationalUnit updated = unit.withDetails(
                    cmd.code() != null ? cmd.code() : unit.code(),
                    cmd.name() != null ? cmd.name() : unit.name(),
                    cmd.description() != null ? cmd.description() : unit.description());
            if (cmd.values() != null) {
                UnitTypeDefinition type = cfg.findType(unit.typeCode()).orElseThrow(() ->
                        new ValidationException("Le type « " + unit.typeCode() + " » n'existe pas"));
                updated = updated.withValues(UnitValues.build(type, cmd.values()));
            }
            return new ConfigurationHolder.Change<>(cfg.replaceUnit(updated), updated);
        });
    }

    @Override
    public OrganizationalUnit moveUnit(String id, String newParentId) {
        return holder.update(cfg -> {
            OrganizationalUnit unit = require(cfg, id);
            String target = Texts.blankToNull(newParentId);
            if (target != null) {
                if (target.equals(id)) {
                    throw new ValidationException("Une unité ne peut pas être son propre parent");
                }
                OrganizationalUnit newParent = cfg.findUnit(target).orElseThrow(() ->
                        new NotFoundException("Nouveau parent introuvable : " + target));
                boolean underDescendant = descendants(cfg, id).stream().anyMatch(d -> d.id().equals(newParent.id()));
                if (underDescendant) {
                    throw new ValidationException("Impossible de déplacer « " + unit.name()
                            + " » sous l'un de ses descendants (« " + newParent.name() + " ») : cela créerait un cycle");
                }
            }
            OrganizationalUnit moved = unit.withParentId(target);
            return new ConfigurationHolder.Change<>(cfg.replaceUnit(moved), moved);
        });
    }

    @Override
    public OrganizationalUnit deactivateUnit(String id, boolean cascade) {
        return holder.update(cfg -> {
            OrganizationalUnit unit = require(cfg, id);
            Set<String> targets = new HashSet<>();
            targets.add(id);
            if (cascade) {
                descendants(cfg, id).forEach(d -> targets.add(d.id()));
            } else {
                long activeChildren = cfg.childrenOf(id).stream().filter(OrganizationalUnit::active).count();
                if (activeChildren > 0) {
                    throw new ValidationException("L'unité « " + unit.name() + " » a " + activeChildren
                            + " enfant(s) actif(s) : les désactiver d'abord ou utiliser la désactivation en cascade");
                }
            }
            List<OrganizationalUnit> units = new ArrayList<>();
            OrganizationalUnit result = null;
            for (OrganizationalUnit u : cfg.units()) {
                if (targets.contains(u.id())) {
                    u = u.withActive(false);
                }
                if (u.id().equals(id)) {
                    result = u;
                }
                units.add(u);
            }
            return new ConfigurationHolder.Change<>(cfg.withUnits(units), result);
        });
    }

    @Override
    public OrganizationalUnit activateUnit(String id) {
        return holder.update(cfg -> {
            OrganizationalUnit unit = require(cfg, id);
            if (unit.parentId() != null) {
                cfg.findUnit(unit.parentId()).filter(p -> !p.active()).ifPresent(p -> {
                    throw new ValidationException("L'unité « " + unit.name() + " » ne peut pas être réactivée : "
                            + "son parent « " + p.name() + " » est inactif");
                });
            }
            OrganizationalUnit activated = unit.withActive(true);
            return new ConfigurationHolder.Change<>(cfg.replaceUnit(activated), activated);
        });
    }

    @Override
    public void deleteUnit(String id) {
        holder.update(cfg -> {
            OrganizationalUnit unit = require(cfg, id);
            int children = cfg.childrenOf(id).size();
            if (children > 0) {
                throw new ValidationException("L'unité « " + unit.name() + " » a " + children
                        + " enfant(s) : suppression refusée pour ne pas créer d'orphelins");
            }
            List<OrganizationalUnit> units = new ArrayList<>(cfg.units());
            units.removeIf(u -> u.id().equals(id));
            return new ConfigurationHolder.Change<>(cfg.withUnits(units), null);
        });
    }

    // ------------------------------------------------------------ utilitaires

    private static OrganizationalUnit require(InstitutionConfig cfg, String id) {
        return cfg.findUnit(id).orElseThrow(() -> new NotFoundException("Unité introuvable : " + id));
    }

    private static List<OrganizationalUnit> ancestors(InstitutionConfig cfg, OrganizationalUnit unit) {
        List<OrganizationalUnit> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(unit.id());
        OrganizationalUnit current = unit;
        while (current.parentId() != null) {
            Optional<OrganizationalUnit> parent = cfg.findUnit(current.parentId());
            if (parent.isEmpty() || !visited.add(parent.get().id())) {
                break;
            }
            result.add(parent.get());
            current = parent.get();
        }
        return List.copyOf(result);
    }

    private static List<OrganizationalUnit> descendants(InstitutionConfig cfg, String id) {
        Map<String, List<OrganizationalUnit>> byParent = new HashMap<>();
        for (OrganizationalUnit u : cfg.units()) {
            if (u.parentId() != null) {
                byParent.computeIfAbsent(u.parentId(), k -> new ArrayList<>()).add(u);
            }
        }
        List<OrganizationalUnit> result = new ArrayList<>();
        collect(byParent, id, result, new HashSet<>(Set.of(id)));
        return List.copyOf(result);
    }

    private static void collect(Map<String, List<OrganizationalUnit>> byParent, String id,
                                List<OrganizationalUnit> out, Set<String> visited) {
        for (OrganizationalUnit child : byParent.getOrDefault(id, List.of())) {
            if (visited.add(child.id())) {
                out.add(child);
                collect(byParent, child.id(), out, visited);
            }
        }
    }
}
