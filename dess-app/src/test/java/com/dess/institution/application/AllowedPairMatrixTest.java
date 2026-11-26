package com.dess.institution.application;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief The allowed parent-child matrix can be edited and every change is validated (issue #134).
 *
 * <p>The university fixture allows UNIVERSITE → FACULTE → MENTION → PARCOURS → NIVEAU.</p>
 */
class AllowedPairMatrixTest
{
    @TempDir
    Path tmp;

    UnitTypeService types;
    HierarchyService hierarchy;
    List<AllowedParentChildPair> initialPairs;

    @BeforeEach
    void setUp()
    {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        types = module.unitTypeService();
        hierarchy = module.hierarchyService();
        initialPairs = types.getAllowedPairs();
    }

    @Test
    void refusesATypeAsItsOwnParent()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> types.addAllowedPair("FACULTE", "FACULTE"));

        assertEquals(List.of("Allowed relation FACULTE → FACULTE: a type cannot be its own parent"),
                     e.getViolations());
        assertFalse(types.isAllowed("FACULTE", "FACULTE"));
        assertEquals(initialPairs, types.getAllowedPairs());
    }

    @Test
    void refusesAPairWithAnUnknownParentAndNamesIt()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> types.addAllowedPair("GHOST", "MENTION"));

        assertTrue(e.getViolations().contains(
            "Allowed relation GHOST → MENTION: parent type 'GHOST' does not exist"), e.getViolations().toString());
        assertEquals(initialPairs, types.getAllowedPairs());
    }

    @Test
    void refusesAPairWithAnUnknownChildAndNamesIt()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> types.addAllowedPair("FACULTE", "GHOST"));

        assertTrue(e.getViolations().contains(
            "Allowed relation FACULTE → GHOST: child type 'GHOST' does not exist"), e.getViolations().toString());
    }

    @Test
    void refusesADuplicatePair()
    {
        assertThrows(ValidationException.class, () -> types.addAllowedPair("FACULTE", "MENTION"));
        assertEquals(initialPairs, types.getAllowedPairs());
    }

    @Test
    void anAddedPairChangesTheHierarchyImmediately()
    {
        // Before: a level directly under a faculty is forbidden.
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("LAW_L1", "Law year 1", "NIVEAU", "faculte-droit", null)));

        types.addAllowedPair("FACULTE", "NIVEAU");

        OrganizationalUnit level = hierarchy.createUnit(
            CreateUnitCommand.of("LAW_L1", "Law year 1", "NIVEAU", "faculte-droit", null));

        assertEquals("faculte-droit", level.parentId());
        assertTrue(types.getAllowedChildTypes("FACULTE").contains("NIVEAU"));
    }

    @Test
    void aRemovedPairForbidsNewChildrenImmediately()
    {
        types.create(UnitTypeDefinition.of("LABORATORY", "Laboratory"));
        types.addAllowedPair("FACULTE", "LABORATORY");
        types.removeAllowedPair("FACULTE", "LABORATORY");

        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("LAB_1", "Lab 1", "LABORATORY", "faculte-droit", null)));

        assertTrue(e.getMessage().contains("forbidden relation"), e.getMessage());
        assertEquals(initialPairs, types.getAllowedPairs());
    }

    @Test
    void aPairUsedByUnitsCannotBeRemoved()
    {
        assertThrows(ValidationException.class, () -> types.removeAllowedPair("UNIVERSITE", "FACULTE"));
        assertTrue(types.isAllowed("UNIVERSITE", "FACULTE"));
    }
}
