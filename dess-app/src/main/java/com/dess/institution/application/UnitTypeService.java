package com.dess.institution.application;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.UnitTypeDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Gestion des types d'unités définis dynamiquement et des relations parent-enfant autorisées.
 * Les définitions d'attributs d'un type se gèrent via {@link AttributeService}.
 */
public interface UnitTypeService {

    /**
     * Crée un type. Le code doit être unique ; le type peut déjà porter des définitions d'attributs.
     *
     * @throws com.dess.institution.exception.ValidationException si le code existe déjà ou est invalide
     */
    UnitTypeDefinition create(UnitTypeDefinition type);

    /** Modifie le libellé et la description. Le code d'un type est immuable. */
    UnitTypeDefinition update(String code, String label, String description);

    Optional<UnitTypeDefinition> getByCode(String code);

    List<UnitTypeDefinition> getAll();

    /** Un type inactif n'accepte plus de nouvelles unités ; les unités existantes sont conservées. */
    UnitTypeDefinition deactivate(String code);

    UnitTypeDefinition activate(String code);

    /**
     * Supprime un type.
     *
     * @throws com.dess.institution.exception.ValidationException si des unités l'utilisent encore ou si
     *                                                            des relations autorisées le référencent
     */
    void delete(String code);

    // ---- relations parent-enfant autorisées ----

    List<AllowedParentChildPair> getAllowedPairs();

    /** Autorise les unités de type {@code childType} sous des unités de type {@code parentType}. */
    AllowedParentChildPair addAllowedPair(String parentType, String childType);

    /**
     * Retire une relation autorisée.
     *
     * @throws com.dess.institution.exception.ValidationException si des unités existantes l'utilisent
     */
    void removeAllowedPair(String parentType, String childType);

    boolean isAllowed(String parentType, String childType);

    List<String> getAllowedChildTypes(String parentType);

    List<String> getAllowedParentTypes(String childType);
}
