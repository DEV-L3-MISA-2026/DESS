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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @brief Default implementation of {@link HierarchyService}.
 *
 * <p>Tree traversal is delegated to {@link InstitutionConfig}; this class focuses on use-case
 * orchestration, validation entry points and immutable updates through
 * {@link ConfigurationHolder#update(java.util.function.Function)}.</p>
 */
public final class HierarchyServiceImpl implements HierarchyService
{
    private final ConfigurationHolder holder;

    /**
     * Creates the service.
     *
     * @param holder configuration holder shared by the module
     * @throws NullPointerException if {@code holder} is {@code null}
     */
    public HierarchyServiceImpl(ConfigurationHolder holder)
    {
        this.holder = Objects.requireNonNull(holder, "holder");
    }

    @Override
    public Optional<OrganizationalUnit> getById(String id)
    {
        return holder.current().findUnit(id);
    }

    @Override
    public Optional<OrganizationalUnit> getByCode(String code)
    {
        return holder.current().findUnitByCode(code);
    }

    @Override
    public List<OrganizationalUnit> getRoots()
    {
        return holder.current()
                     .units()
                     .stream()
                     .filter(OrganizationalUnit::isRoot)
                     .toList();
    }

    @Override
    public List<OrganizationalUnit> getChildren(String id)
    {
        InstitutionConfig configuration = holder.current();
        require(configuration, id);

        return configuration.childrenOf(id);
    }

    @Override
    public Optional<OrganizationalUnit> getParent(String id)
    {
        InstitutionConfig configuration = holder.current();
        OrganizationalUnit unit = require(configuration, id);

        return unit.parentId() == null
            ? Optional.empty()
            : configuration.findUnit(unit.parentId());
    }

    @Override
    public List<OrganizationalUnit> getDescendants(String id)
    {
        InstitutionConfig configuration = holder.current();
        require(configuration, id);

        return configuration.descendantsOf(id);
    }

    @Override
    public List<OrganizationalUnit> getAncestors(String id)
    {
        InstitutionConfig configuration = holder.current();

        return configuration.ancestorsOf(require(configuration, id));
    }

    @Override
    public List<OrganizationalUnit> getPath(String id)
    {
        InstitutionConfig configuration = holder.current();

        return configuration.pathOf(require(configuration, id));
    }

    @Override
    public List<OrganizationalUnit> getByType(String typeCode)
    {
        return holder.current()
                     .units()
                     .stream()
                     .filter(unit -> unit.typeCode().equals(typeCode))
                     .toList();
    }

    @Override
    public List<OrganizationalUnit> getSubtree(String id)
    {
        InstitutionConfig configuration = holder.current();
        OrganizationalUnit unit = require(configuration, id);

        List<OrganizationalUnit> result = new ArrayList<>();
        result.add(unit);
        result.addAll(configuration.descendantsOf(id));

        return List.copyOf(result);
    }

    @Override
    public boolean isDescendantOf(String candidateId, String ancestorId)
    {
        InstitutionConfig configuration = holder.current();
        OrganizationalUnit candidate = require(configuration, candidateId);
        require(configuration, ancestorId);

        return configuration.ancestorsOf(candidate)
                            .stream()
                            .anyMatch(ancestor -> ancestor.id().equals(ancestorId));
    }

    @Override
    public boolean areInSameBranch(String idA, String idB)
    {
        if (idA.equals(idB))
        {
            require(holder.current(), idA);
            return true;
        }

        return isDescendantOf(idA, idB) || isDescendantOf(idB, idA);
    }

    @Override
    public Optional<OrganizationalUnit> getNearestCommonAncestor(String idA, String idB)
    {
        InstitutionConfig configuration = holder.current();
        List<OrganizationalUnit> pathA = configuration.pathOf(require(configuration, idA));
        List<OrganizationalUnit> pathB = configuration.pathOf(require(configuration, idB));
        int commonLength = 0;

        while (commonLength < pathA.size()
               && commonLength < pathB.size()
               && pathA.get(commonLength).id().equals(pathB.get(commonLength).id()))
        {
            commonLength++;
        }

        return commonLength == 0
            ? Optional.empty()
            : Optional.of(pathA.get(commonLength - 1));
    }

    @Override
    public OrganizationalUnit createUnit(CreateUnitCommand command)
    {
        return holder.update(configuration ->
        {
            UnitTypeDefinition type = configuration.findType(Texts.orEmpty(command.typeCode()))
                                                   .orElseThrow(() -> new ValidationException(
                                                       "Unit type '" + command.typeCode() + "' does not exist"
                                                   ));

            if (!type.active())
            {
                throw new ValidationException("Type '" + type.code() + "' is inactive: "
                                              + "new units cannot be created under it");
            }

            String id = Texts.isBlank(command.id()) ? UUID.randomUUID().toString() : command.id();
            OrganizationalUnit unit = new OrganizationalUnit(
                id,
                command.code(),
                command.name(),
                command.description(),
                type.code(),
                command.parentId(),
                command.active(),
                UnitValues.build(type, command.values())
            );

            List<OrganizationalUnit> units = new ArrayList<>(configuration.units());
            units.add(unit);

            return new ConfigurationHolder.Change<>(configuration.withUnits(units), unit);
        });
    }

    @Override
    public OrganizationalUnit updateUnit(String id, UpdateUnitCommand command)
    {
        return holder.update(configuration ->
        {
            OrganizationalUnit unit = require(configuration, id);
            OrganizationalUnit updated = unit.withDetails(
                Objects.requireNonNullElse(command.code(), unit.code()),
                Objects.requireNonNullElse(command.name(), unit.name()),
                command.description() != null ? command.description() : unit.description()
            );

            if (command.values() != null)
            {
                UnitTypeDefinition type = configuration.findType(unit.typeCode())
                                                       .orElseThrow(() -> new ValidationException(
                                                           "Type '" + unit.typeCode() + "' does not exist"
                                                       ));

                updated = updated.withValues(UnitValues.build(type, command.values()));
            }

            return new ConfigurationHolder.Change<>(configuration.replaceUnit(updated), updated);
        });
    }

    @Override
    public OrganizationalUnit moveUnit(String id, String newParentId)
    {
        return holder.update(configuration ->
        {
            OrganizationalUnit unit = require(configuration, id);
            String targetId = Texts.blankToNull(newParentId);

            if (targetId != null)
            {
                if (targetId.equals(id))
                {
                    throw new ValidationException("A unit cannot be its own parent");
                }

                OrganizationalUnit newParent = configuration.findUnit(targetId)
                                                            .orElseThrow(() -> new NotFoundException(
                                                                "New parent not found: " + targetId
                                                            ));
                boolean underDescendant = configuration.descendantsOf(id)
                                                       .stream()
                                                       .anyMatch(descendant -> descendant.id().equals(newParent.id()));

                if (underDescendant)
                {
                    throw new ValidationException("Cannot move '" + unit.name()
                                                  + "' under one of its descendants ('" + newParent.name()
                                                  + "'): this would create a cycle");
                }
            }

            OrganizationalUnit moved = unit.withParentId(targetId);

            return new ConfigurationHolder.Change<>(configuration.replaceUnit(moved), moved);
        });
    }

    @Override
    public OrganizationalUnit deactivateUnit(String id, boolean cascade)
    {
        return holder.update(configuration ->
        {
            OrganizationalUnit unit = require(configuration, id);
            Set<String> targetIds = new HashSet<>();
            targetIds.add(id);

            if (cascade)
            {
                configuration.descendantsOf(id)
                             .forEach(descendant -> targetIds.add(descendant.id()));
            }
            else
            {
                long activeChildren = configuration.childrenOf(id)
                                                     .stream()
                                                     .filter(OrganizationalUnit::active)
                                                     .count();

                if (activeChildren > 0)
                {
                    throw new ValidationException("Unit '" + unit.name() + "' has " + activeChildren
                                                  + " active child(ren): deactivate them first or use "
                                                  + "cascade deactivation");
                }
            }

            List<OrganizationalUnit> units = new ArrayList<>();
            OrganizationalUnit result = null;

            for (OrganizationalUnit current : configuration.units())
            {
                if (targetIds.contains(current.id()))
                {
                    current = current.withActive(false);
                }

                if (current.id().equals(id))
                {
                    result = current;
                }

                units.add(current);
            }

            return new ConfigurationHolder.Change<>(configuration.withUnits(units), result);
        });
    }

    @Override
    public OrganizationalUnit activateUnit(String id)
    {
        return holder.update(configuration ->
        {
            OrganizationalUnit unit = require(configuration, id);
            OrganizationalUnit parent = unit.parentId() == null
                ? null
                : configuration.findUnit(unit.parentId()).orElse(null);

            if (parent != null && !parent.active())
            {
                throw new ValidationException("Unit '" + unit.name() + "' cannot be reactivated: "
                                              + "its parent '" + parent.name() + "' is inactive");
            }

            OrganizationalUnit activated = unit.withActive(true);

            return new ConfigurationHolder.Change<>(configuration.replaceUnit(activated), activated);
        });
    }

    @Override
    public void deleteUnit(String id)
    {
        holder.update(configuration ->
        {
            OrganizationalUnit unit = require(configuration, id);
            int children = configuration.childrenOf(id).size();

            if (children > 0)
            {
                throw new ValidationException("Unit '" + unit.name() + "' has " + children
                                              + " child(ren): deletion refused to avoid orphans");
            }

            List<OrganizationalUnit> units = new ArrayList<>(configuration.units());
            units.removeIf(current -> current.id().equals(id));

            return new ConfigurationHolder.Change<>(configuration.withUnits(units), null);
        });
    }

    private static OrganizationalUnit require(InstitutionConfig configuration, String id)
    {
        return configuration.findUnit(id)
                            .orElseThrow(() -> new NotFoundException("Unit not found: " + id));
    }
}
