package com.dess.institution.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Configuration complète (et immuable) d'un établissement : informations générales, types d'unités,
 * relations parent-enfant autorisées et unités organisationnelles.
 * <p>
 * Les unités sont stockées à plat ; la hiérarchie est portée par {@link OrganizationalUnit#parentId()}.
 * Toute modification produit une nouvelle instance (copie), ce qui permet de valider un état complet
 * avant de le persister puis de le publier.
 */
public record InstitutionConfig(
        String id,
        String code,
        String name,
        String description,
        List<UnitTypeDefinition> unitTypes,
        List<AllowedParentChildPair> allowedPairs,
        List<OrganizationalUnit> units) {

    public InstitutionConfig {
        id = Texts.orEmpty(id);
        code = Texts.orEmpty(code);
        name = Texts.orEmpty(name);
        description = Texts.blankToNull(description);
        unitTypes = unitTypes == null ? List.of() : List.copyOf(unitTypes);
        allowedPairs = allowedPairs == null ? List.of() : List.copyOf(allowedPairs);
        units = units == null ? List.of() : List.copyOf(units);
    }

    public static InstitutionConfig empty(String id, String code, String name, String description) {
        return new InstitutionConfig(id, code, name, description, List.of(), List.of(), List.of());
    }

    // ---- recherches ----

    public Optional<UnitTypeDefinition> findType(String typeCode) {
        return unitTypes.stream().filter(t -> t.code().equals(typeCode)).findFirst();
    }

    public Optional<OrganizationalUnit> findUnit(String unitId) {
        return units.stream().filter(u -> u.id().equals(unitId)).findFirst();
    }

    public Optional<OrganizationalUnit> findUnitByCode(String unitCode) {
        return units.stream().filter(u -> u.code().equals(unitCode)).findFirst();
    }

    public boolean isAllowed(String parentTypeCode, String childTypeCode) {
        return allowedPairs.stream()
                .anyMatch(p -> p.parent().equals(parentTypeCode) && p.child().equals(childTypeCode));
    }

    public List<OrganizationalUnit> childrenOf(String unitId) {
        return units.stream().filter(u -> unitId.equals(u.parentId())).toList();
    }

    // ---- copies modifiées ----

    public InstitutionConfig withInfo(String newCode, String newName, String newDescription) {
        return new InstitutionConfig(id, newCode, newName, newDescription, unitTypes, allowedPairs, units);
    }

    public InstitutionConfig withUnitTypes(List<UnitTypeDefinition> newTypes) {
        return new InstitutionConfig(id, code, name, description, newTypes, allowedPairs, units);
    }

    public InstitutionConfig withAllowedPairs(List<AllowedParentChildPair> newPairs) {
        return new InstitutionConfig(id, code, name, description, unitTypes, newPairs, units);
    }

    public InstitutionConfig withUnits(List<OrganizationalUnit> newUnits) {
        return new InstitutionConfig(id, code, name, description, unitTypes, allowedPairs, newUnits);
    }

    /** Remplace (par identifiant) une unité existante en conservant sa position dans la liste. */
    public InstitutionConfig replaceUnit(OrganizationalUnit updated) {
        List<OrganizationalUnit> copy = new ArrayList<>(units.size());
        for (OrganizationalUnit u : units) {
            copy.add(u.id().equals(updated.id()) ? updated : u);
        }
        return withUnits(copy);
    }

    /** Remplace (par code) un type existant en conservant sa position dans la liste. */
    public InstitutionConfig replaceType(UnitTypeDefinition updated) {
        List<UnitTypeDefinition> copy = new ArrayList<>(unitTypes.size());
        for (UnitTypeDefinition t : unitTypes) {
            copy.add(t.code().equals(updated.code()) ? updated : t);
        }
        return withUnitTypes(copy);
    }
}
