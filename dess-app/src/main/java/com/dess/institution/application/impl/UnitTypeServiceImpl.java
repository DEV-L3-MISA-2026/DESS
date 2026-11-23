package com.dess.institution.application.impl;

import com.dess.institution.application.UnitTypeService;
import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;
import com.dess.institution.validation.InstitutionValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @brief Default implementation of {@link UnitTypeService}.
 *
 * <p>The service validates type codes, protects types that are still referenced by units or allowed
 * relations, and delegates every write to {@link ConfigurationHolder#update(java.util.function.Function)}.</p>
 */
public final class UnitTypeServiceImpl implements UnitTypeService
{
    private final ConfigurationHolder holder;

    /**
     * Creates the service.
     *
     * @param holder configuration holder shared by the module
     * @throws NullPointerException if {@code holder} is {@code null}
     */
    public UnitTypeServiceImpl(ConfigurationHolder holder)
    {
        this.holder = Objects.requireNonNull(holder, "holder");
    }

    @Override
    public UnitTypeDefinition create(UnitTypeDefinition type)
    {
        return holder.update(configuration ->
        {
            String key = InstitutionValidator.typeKey(type.code());
            Optional<UnitTypeDefinition> existing = configuration.unitTypes()
                                                                 .stream()
                                                                 .filter(current -> InstitutionValidator.typeKey(current.code())
                                                                                                        .equals(key))
                                                                 .findFirst();

            if (existing.isPresent())
            {
                throw new ValidationException(InstitutionValidator.duplicateTypeMessage(type.code(),
                                                                                        existing.get().code()));
            }

            List<UnitTypeDefinition> types = new ArrayList<>(configuration.unitTypes());
            types.add(type);

            return new ConfigurationHolder.Change<>(configuration.withUnitTypes(types), type);
        });
    }

    @Override
    public UnitTypeDefinition update(String code, String label, String description)
    {
        return holder.update(configuration ->
        {
            UnitTypeDefinition updated = require(configuration, code)
                .withLabelAndDescription(label, description);

            return new ConfigurationHolder.Change<>(configuration.replaceType(updated), updated);
        });
    }

    @Override
    public Optional<UnitTypeDefinition> getByCode(String code)
    {
        return holder.current().findType(code);
    }

    @Override
    public List<UnitTypeDefinition> getAll()
    {
        return holder.current().unitTypes();
    }

    @Override
    public UnitTypeDefinition deactivate(String code)
    {
        return setActive(code, false);
    }

    @Override
    public UnitTypeDefinition activate(String code)
    {
        return setActive(code, true);
    }

    @Override
    public void delete(String code)
    {
        holder.update(configuration ->
        {
            require(configuration, code);

            long usedBy = configuration.units()
                                       .stream()
                                       .filter(unit -> unit.typeCode().equals(code))
                                       .count();

            if (usedBy > 0)
            {
                throw new ValidationException("Unit type '" + code + "' is used by " + usedBy
                                              + " unit(s): deletion refused");
            }

            List<String> pairs = configuration.allowedPairs()
                                              .stream()
                                              .filter(pair -> pair.parent().equals(code)
                                                              || pair.child().equals(code))
                                              .map(pair -> pair.parent() + " → " + pair.child())
                                              .toList();

            if (!pairs.isEmpty())
            {
                throw new ValidationException("Unit type '" + code + "' is referenced by allowed relations ("
                                              + String.join(", ", pairs)
                                              + "): remove them first");
            }

            List<UnitTypeDefinition> types = new ArrayList<>(configuration.unitTypes());
            types.removeIf(current -> current.code().equals(code));

            return new ConfigurationHolder.Change<>(configuration.withUnitTypes(types), null);
        });
    }

    @Override
    public List<AllowedParentChildPair> getAllowedPairs()
    {
        return holder.current().allowedPairs();
    }

    @Override
    public AllowedParentChildPair addAllowedPair(String parentType, String childType)
    {
        return holder.update(configuration ->
        {
            AllowedParentChildPair pair = new AllowedParentChildPair(parentType, childType);
            List<AllowedParentChildPair> pairs = new ArrayList<>(configuration.allowedPairs());
            pairs.add(pair);

            return new ConfigurationHolder.Change<>(configuration.withAllowedPairs(pairs), pair);
        });
    }

    @Override
    public void removeAllowedPair(String parentType, String childType)
    {
        holder.update(configuration ->
        {
            AllowedParentChildPair pair = new AllowedParentChildPair(parentType, childType);

            if (!configuration.allowedPairs().contains(pair))
            {
                throw new NotFoundException("Allowed relation not found: " + parentType + " → " + childType);
            }

            List<AllowedParentChildPair> pairs = new ArrayList<>(configuration.allowedPairs());
            pairs.remove(pair);

            return new ConfigurationHolder.Change<>(configuration.withAllowedPairs(pairs), null);
        });
    }

    @Override
    public boolean isAllowed(String parentType, String childType)
    {
        return holder.current().isAllowed(parentType, childType);
    }

    @Override
    public List<String> getAllowedChildTypes(String parentType)
    {
        return holder.current()
                     .allowedPairs()
                     .stream()
                     .filter(pair -> pair.parent().equals(parentType))
                     .map(AllowedParentChildPair::child)
                     .toList();
    }

    @Override
    public List<String> getAllowedParentTypes(String childType)
    {
        return holder.current()
                     .allowedPairs()
                     .stream()
                     .filter(pair -> pair.child().equals(childType))
                     .map(AllowedParentChildPair::parent)
                     .toList();
    }

    private UnitTypeDefinition setActive(String code, boolean active)
    {
        return holder.update(configuration ->
        {
            UnitTypeDefinition updated = require(configuration, code).withActive(active);

            return new ConfigurationHolder.Change<>(configuration.replaceType(updated), updated);
        });
    }

    private static UnitTypeDefinition require(InstitutionConfig configuration, String code)
    {
        return configuration.findType(code)
                            .orElseThrow(() -> new NotFoundException("Unit type not found: " + code));
    }
}
