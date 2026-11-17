package com.dess.institution.application;

import com.dess.institution.domain.OrganizationalUnit;

import java.util.List;
import java.util.Optional;

/**
 * Service central du module : consultation et modification de l'arbre des unités organisationnelles.
 * <p>
 * Les listes retournées sont des copies immuables. Les identifiants d'unités sont stables : un
 * déplacement ne les modifie jamais. Tout type d'unité peut être racine ; seule la relation
 * parent-enfant est contrainte par les paires autorisées.
 * <p>
 * Les méthodes qui prennent un identifiant d'unité lèvent
 * {@link com.dess.institution.exception.NotFoundException} si l'unité n'existe pas, sauf
 * {@code getById} et {@code getByCode} qui retournent un {@code Optional}.
 */
public interface HierarchyService {

    // ------------------------------------------------------------ consultation

    Optional<OrganizationalUnit> getById(String id);

    Optional<OrganizationalUnit> getByCode(String code);

    /** Unités sans parent. */
    List<OrganizationalUnit> getRoots();

    List<OrganizationalUnit> getChildren(String id);

    Optional<OrganizationalUnit> getParent(String id);

    /** Tous les descendants (parcours en profondeur), sans l'unité elle-même. */
    List<OrganizationalUnit> getDescendants(String id);

    /** Ancêtres du plus proche (le parent) jusqu'à la racine. */
    List<OrganizationalUnit> getAncestors(String id);

    /** Chemin de la racine jusqu'à l'unité incluse. */
    List<OrganizationalUnit> getPath(String id);

    /** Toutes les unités d'un type donné (par code de type), actives ou non. */
    List<OrganizationalUnit> getByType(String typeCode);

    /** L'unité elle-même suivie de tous ses descendants. */
    List<OrganizationalUnit> getSubtree(String id);

    /** {@code true} si {@code candidateId} est un descendant strict de {@code ancestorId}. */
    boolean isDescendantOf(String candidateId, String ancestorId);

    /** {@code true} si les deux unités sont identiques ou si l'une est ancêtre de l'autre. */
    boolean areInSameBranch(String idA, String idB);

    /**
     * Plus proche ancêtre commun. Une unité est considérée comme son propre ancêtre : si l'une est
     * ancêtre de l'autre, c'est elle qui est retournée. Vide si les unités sont dans des arbres distincts.
     */
    Optional<OrganizationalUnit> getNearestCommonAncestor(String idA, String idB);

    // ------------------------------------------------------------ modification

    /**
     * Crée une unité. Les valeurs d'attributs sont validées selon le type ; les valeurs par défaut
     * sont appliquées aux attributs non renseignés.
     *
     * @throws com.dess.institution.exception.ValidationException type inconnu ou inactif, relation
     *                                                            parent-enfant interdite, code dupliqué, valeur invalide, attribut obligatoire manquant...
     */
    OrganizationalUnit createUnit(CreateUnitCommand command);

    OrganizationalUnit updateUnit(String id, UpdateUnitCommand command);

    /**
     * Déplace l'unité sous un nouveau parent ({@code null} pour la rendre racine). L'identifiant ne change pas.
     *
     * @throws com.dess.institution.exception.ValidationException relation interdite, cycle, déplacement
     *                                                            sous soi-même ou sous un descendant
     */
    OrganizationalUnit moveUnit(String id, String newParentId);

    /**
     * Désactive l'unité. Sans {@code cascade}, l'opération est refusée si elle a des enfants actifs ;
     * avec {@code cascade}, tous ses descendants sont désactivés.
     */
    OrganizationalUnit deactivateUnit(String id, boolean cascade);

    /** Réactive l'unité ; refusé si son parent est inactif. */
    OrganizationalUnit activateUnit(String id);

    /** Supprime une unité sans enfant (jamais d'orphelins). */
    void deleteUnit(String id);
}
