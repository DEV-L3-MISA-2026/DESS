package com.dess.institution.application;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchyServiceTest {

    @TempDir
    Path tmp;

    HierarchyService hierarchy;
    UnitTypeService types;

    @BeforeEach
    void setUp() {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        hierarchy = module.hierarchyService();
        types = module.unitTypeService();
    }

    private static List<String> ids(List<OrganizationalUnit> units) {
        return units.stream().map(OrganizationalUnit::id).toList();
    }

    // ---------------------------------------------------------------- consultation

    @Test
    void findsByIdAndByCode() {
        assertEquals("Mention Droit", hierarchy.getById("mention-droit").orElseThrow().name());
        assertEquals("mention-droit", hierarchy.getByCode("MENTION_DROIT").orElseThrow().id());
        assertTrue(hierarchy.getById("nope").isEmpty());
        assertTrue(hierarchy.getByCode("NOPE").isEmpty());
    }

    @Test
    void returnsRootsChildrenAndParent() {
        assertEquals(List.of("univ-x"), ids(hierarchy.getRoots()));
        assertEquals(List.of("faculte-droit", "faculte-sciences"), ids(hierarchy.getChildren("univ-x")));
        assertEquals("faculte-droit", hierarchy.getParent("mention-droit").orElseThrow().id());
        assertTrue(hierarchy.getParent("univ-x").isEmpty());
    }

    @Test
    void returnsDescendantsInDepthFirstOrder() {
        assertEquals(List.of("mention-informatique", "parcours-dev", "dev-l3", "dev-m1"),
                ids(hierarchy.getDescendants("faculte-sciences")));
        assertTrue(hierarchy.getDescendants("dev-l3").isEmpty());
        assertEquals(10, hierarchy.getDescendants("univ-x").size());
    }

    @Test
    void returnsAncestorsNearestFirstAndPathFromRoot() {
        assertEquals(List.of("parcours-dev", "mention-informatique", "faculte-sciences", "univ-x"),
                ids(hierarchy.getAncestors("dev-l3")));
        assertEquals(List.of("univ-x", "faculte-sciences", "mention-informatique", "parcours-dev", "dev-l3"),
                ids(hierarchy.getPath("dev-l3")));
        assertEquals(List.of("univ-x"), ids(hierarchy.getPath("univ-x")));
    }

    @Test
    void returnsUnitsOfAType() {
        assertEquals(List.of("mention-droit", "mention-informatique"), ids(hierarchy.getByType("MENTION")));
        assertTrue(hierarchy.getByType("INCONNU").isEmpty());
    }

    @Test
    void returnsSubtreeIncludingTheUnit() {
        assertEquals(List.of("parcours-droit-prive", "droit-prive-l1", "droit-prive-l2"),
                ids(hierarchy.getSubtree("parcours-droit-prive")));
    }

    @Test
    void answersBranchQuestions() {
        assertTrue(hierarchy.isDescendantOf("dev-l3", "faculte-sciences"));
        assertFalse(hierarchy.isDescendantOf("faculte-sciences", "dev-l3"));
        assertFalse(hierarchy.isDescendantOf("dev-l3", "dev-l3"));

        assertTrue(hierarchy.areInSameBranch("dev-l3", "univ-x"));
        assertTrue(hierarchy.areInSameBranch("univ-x", "dev-l3"));
        assertTrue(hierarchy.areInSameBranch("dev-l3", "dev-l3"));
        assertFalse(hierarchy.areInSameBranch("dev-l3", "droit-prive-l1"));
    }

    @Test
    void findsNearestCommonAncestor() {
        assertEquals("univ-x", hierarchy.getNearestCommonAncestor("dev-l3", "droit-prive-l1").orElseThrow().id());
        assertEquals("parcours-dev", hierarchy.getNearestCommonAncestor("dev-l3", "dev-m1").orElseThrow().id());
        assertEquals("mention-informatique",
                hierarchy.getNearestCommonAncestor("dev-l3", "mention-informatique").orElseThrow().id());
    }

    @Test
    void unknownIdsRaiseNotFound() {
        assertThrows(NotFoundException.class, () -> hierarchy.getChildren("nope"));
        assertThrows(NotFoundException.class, () -> hierarchy.getDescendants("nope"));
        assertThrows(NotFoundException.class, () -> hierarchy.getPath("nope"));
        assertThrows(NotFoundException.class, () -> hierarchy.isDescendantOf("nope", "univ-x"));
    }

    // ---------------------------------------------------------------- création

    @Test
    void createsARootUnit() {
        OrganizationalUnit root = hierarchy.createUnit(CreateUnitCommand.of(
                "UNIV_Y", "Université Y", "UNIVERSITE", null, null));
        assertTrue(root.isRoot());
        assertFalse(root.id().isBlank());
        assertEquals(2, hierarchy.getRoots().size());
    }

    @Test
    void createsAChildUnitWithValues() {
        OrganizationalUnit faculty = hierarchy.createUnit(CreateUnitCommand.of(
                "LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "Rabe Marie")));
        assertEquals("univ-x", faculty.parentId());
        assertEquals(faculty, hierarchy.getById(faculty.id()).orElseThrow());
        assertEquals(3, hierarchy.getChildren("univ-x").size());
    }

    @Test
    void keepsAnExplicitIdWhenProvided() {
        OrganizationalUnit u = hierarchy.createUnit(new CreateUnitCommand("mon-id", "UNIV_Z", "Université Z",
                null, "UNIVERSITE", null, true, null));
        assertEquals("mon-id", u.id());
    }

    @Test
    void refusesForbiddenParentChildRelation() {
        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("P_X", "Parcours X", "PARCOURS", "faculte-droit", null)));
        assertTrue(e.getMessage().contains("relation interdite"));
        assertTrue(hierarchy.getByCode("P_X").isEmpty());
    }

    @Test
    void refusesUnknownTypeUnknownParentAndDuplicateCode() {
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("X1", "X", "GHOST", null, null)));
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("X2", "X", "FACULTE", "parent-inexistant", Map.of("doyen", "A"))));
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("DROIT", "Doublon", "FACULTE", "univ-x", Map.of("doyen", "A"))));
        assertEquals(11, hierarchy.getSubtree("univ-x").size());
    }

    @Test
    void refusesActiveChildUnderInactiveParent() {
        hierarchy.deactivateUnit("faculte-droit", true);
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("M_NEW", "Mention neuve", "MENTION", "faculte-droit", Map.of("chefMention", "A"))));
        // en revanche une unité inactive sous un parent inactif est cohérente
        hierarchy.createUnit(new CreateUnitCommand(null, "M_NEW", "Mention neuve", null, "MENTION",
                "faculte-droit", false, Map.of("chefMention", "A")));
        assertTrue(hierarchy.getByCode("M_NEW").isPresent());
    }

    // ---------------------------------------------------------------- modification

    @Test
    void updatesNameCodeAndDescription() {
        OrganizationalUnit updated = hierarchy.updateUnit("faculte-droit",
                new UpdateUnitCommand("DROIT_2", "Faculté de Droit et d'Économie", "Nouvelle description", null));
        assertEquals("DROIT_2", updated.code());
        assertEquals("faculte-droit", updated.id());
        assertEquals("Nouvelle description", hierarchy.getById("faculte-droit").orElseThrow().description());
        assertEquals("Rakoto Jean", hierarchy.getById("faculte-droit").orElseThrow().value("doyen").orElseThrow().value());
    }

    @Test
    void refusesUpdateThatCreatesDuplicateCode() {
        assertThrows(ValidationException.class, () -> hierarchy.updateUnit("faculte-droit",
                new UpdateUnitCommand("SCIENCES", null, null, null)));
        assertEquals("DROIT", hierarchy.getById("faculte-droit").orElseThrow().code());
    }

    @Test
    void movesAUnitAndKeepsItsIdentifierAndSubtree() {
        OrganizationalUnit moved = hierarchy.moveUnit("mention-droit", "faculte-sciences");
        assertEquals("mention-droit", moved.id());
        assertEquals("faculte-sciences", hierarchy.getParent("mention-droit").orElseThrow().id());
        assertEquals(Set.of("mention-informatique", "mention-droit"),
                Set.copyOf(ids(hierarchy.getChildren("faculte-sciences"))));
        assertTrue(hierarchy.getChildren("faculte-droit").isEmpty());
        assertEquals(List.of("parcours-droit-prive", "droit-prive-l1", "droit-prive-l2"),
                ids(hierarchy.getDescendants("mention-droit")));
        assertEquals("univ-x", hierarchy.getPath("droit-prive-l1").get(0).id());
        assertEquals("faculte-sciences", hierarchy.getPath("droit-prive-l1").get(1).id());
    }

    @Test
    void refusesMoveToForbiddenParentType() {
        assertThrows(ValidationException.class, () -> hierarchy.moveUnit("parcours-dev", "faculte-sciences"));
        assertEquals("mention-informatique", hierarchy.getParent("parcours-dev").orElseThrow().id());
    }

    @Test
    void refusesMoveUnderItself() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> hierarchy.moveUnit("faculte-droit", "faculte-droit"));
        assertTrue(e.getMessage().contains("propre parent"));
    }

    @Test
    void refusesMoveUnderADescendantEvenWhenTheTypePairIsAllowed() {
        // on autorise MENTION -> FACULTE pour que seule la règle anti-cycle puisse refuser
        types.addAllowedPair("MENTION", "FACULTE");
        ValidationException e = assertThrows(ValidationException.class,
                () -> hierarchy.moveUnit("faculte-droit", "mention-droit"));
        assertTrue(e.getMessage().contains("descendants"), e.getMessage());
        assertEquals("univ-x", hierarchy.getParent("faculte-droit").orElseThrow().id());
        assertEquals(2, hierarchy.getChildren("univ-x").size());
    }

    @Test
    void canMoveAUnitToBecomeARoot() {
        hierarchy.moveUnit("faculte-sciences", null);
        assertTrue(hierarchy.getParent("faculte-sciences").isEmpty());
        assertEquals(2, hierarchy.getRoots().size());
    }

    @Test
    void deactivationWithoutCascadeIsRefusedWhenChildrenAreActive() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> hierarchy.deactivateUnit("faculte-droit", false));
        assertTrue(e.getMessage().contains("enfant"));
        assertTrue(hierarchy.getById("faculte-droit").orElseThrow().active());
    }

    @Test
    void cascadingDeactivationDisablesTheWholeBranchAndNothingElse() {
        hierarchy.deactivateUnit("faculte-droit", true);
        assertTrue(hierarchy.getSubtree("faculte-droit").stream().noneMatch(OrganizationalUnit::active));
        assertTrue(hierarchy.getById("faculte-sciences").orElseThrow().active());
        assertTrue(hierarchy.getById("univ-x").orElseThrow().active());
    }

    @Test
    void leafCanBeDeactivatedAndReactivated() {
        hierarchy.deactivateUnit("dev-l3", false);
        assertFalse(hierarchy.getById("dev-l3").orElseThrow().active());
        hierarchy.activateUnit("dev-l3");
        assertTrue(hierarchy.getById("dev-l3").orElseThrow().active());
    }

    @Test
    void cannotReactivateAChildWhileItsParentIsInactive() {
        hierarchy.deactivateUnit("faculte-droit", true);
        assertThrows(ValidationException.class, () -> hierarchy.activateUnit("mention-droit"));
        hierarchy.activateUnit("faculte-droit");
        hierarchy.activateUnit("mention-droit");
        assertTrue(hierarchy.getById("mention-droit").orElseThrow().active());
    }

    @Test
    void refusesToDeleteAUnitThatHasChildren() {
        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.deleteUnit("faculte-droit"));
        assertTrue(e.getMessage().contains("orphelins"));
        assertTrue(hierarchy.getById("faculte-droit").isPresent());
        assertTrue(hierarchy.getById("mention-droit").isPresent());
    }

    @Test
    void deletesALeaf() {
        hierarchy.deleteUnit("droit-prive-l2");
        assertTrue(hierarchy.getById("droit-prive-l2").isEmpty());
        assertEquals(List.of("droit-prive-l1"), ids(hierarchy.getChildren("parcours-droit-prive")));
        assertThrows(NotFoundException.class, () -> hierarchy.deleteUnit("droit-prive-l2"));
    }
}
