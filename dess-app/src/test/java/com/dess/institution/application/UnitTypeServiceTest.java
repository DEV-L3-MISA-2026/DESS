package com.dess.institution.application;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief Tests for user-defined unit types and allowed parent-child relations.
 */
class UnitTypeServiceTest
{
    @TempDir
    Path tmp;

    UnitTypeService types;
    HierarchyService hierarchy;

    @BeforeEach
    void setUp()
    {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        types = module.unitTypeService();
        hierarchy = module.hierarchyService();
    }

    @Test
    void createsATypeUnknownInAdvance()
    {
        UnitTypeDefinition created = types.create(UnitTypeDefinition.of("LABORATORY", "Laboratory"));

        assertEquals("LABORATORY", created.code());
        assertTrue(created.active());
        assertEquals(6, types.getAll().size());
    }

    @Test
    void getsByCode()
    {
        assertEquals("Department", types.getByCode("MENTION").orElseThrow().label());
        assertTrue(types.getByCode("UNKNOWN").isEmpty());
    }

    @Test
    void updatesLabelAndDescriptionButNeverTheCode()
    {
        UnitTypeDefinition updated = types.update("MENTION", "Department of Studies", "Field of study");

        assertEquals("MENTION", updated.code());
        assertEquals("Department of Studies", types.getByCode("MENTION").orElseThrow().label());
        assertEquals("Field of study", types.getByCode("MENTION").orElseThrow().description());

        // Attribute definitions are preserved.
        assertEquals(4, types.getByCode("MENTION").orElseThrow().attributes().size());
    }

    @Test
    void updateOfUnknownTypeFails()
    {
        assertThrows(NotFoundException.class, () -> types.update("NOPE", "x", null));
    }

    @Test
    void refusesDuplicateTypeCode()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> types.create(UnitTypeDefinition.of("MENTION", "Another department")));

        assertTrue(e.getMessage().contains("MENTION"));
        assertEquals(5, types.getAll().size());
    }

    @Test
    void refusesDuplicateTypeKeyThatDiffersOnlyByCase()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> types.create(UnitTypeDefinition.of("Mention", "Department")));

        assertEquals(List.of("Type 'Mention': duplicate type key (same key as 'MENTION')"), e.getViolations());
        assertTrue(types.getByCode("Mention").isEmpty());
        assertEquals(5, types.getAll().size());
    }

    @Test
    void refusesBlankLabelOnCreate()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> types.create(UnitTypeDefinition.of("LABORATORY", "  ")));

        assertTrue(e.getViolations().contains("Type 'LABORATORY': label required"), e.getViolations().toString());
        assertTrue(types.getByCode("LABORATORY").isEmpty());
    }

    @Test
    void refusesBlankLabelOnUpdateAndKeepsTheOldOne()
    {
        assertThrows(ValidationException.class, () -> types.update("MENTION", "", "Field of study"));

        assertEquals("Department", types.getByCode("MENTION").orElseThrow().label());
    }

    @Test
    void refusesInvalidTypeCode()
    {
        assertThrows(ValidationException.class, () -> types.create(UnitTypeDefinition.of("bad code", "x")));
        assertThrows(ValidationException.class, () -> types.create(UnitTypeDefinition.of("", "x")));
    }

    @Test
    void deletesAnUnusedType()
    {
        types.create(UnitTypeDefinition.of("LABORATORY", "Laboratory"));
        types.delete("LABORATORY");

        assertTrue(types.getByCode("LABORATORY").isEmpty());
    }

    @Test
    void refusesToDeleteATypeStillUsedByUnits()
    {
        ValidationException e = assertThrows(ValidationException.class, () -> types.delete("MENTION"));

        assertTrue(e.getMessage().contains("used"));
        assertTrue(types.getByCode("MENTION").isPresent());

        // No unit references a non-existent type.
        assertEquals(2, hierarchy.getByType("MENTION").size());
    }

    @Test
    void refusesToDeleteATypeReferencedByAllowedPairs()
    {
        types.create(UnitTypeDefinition.of("LABORATORY", "Laboratory"));
        types.addAllowedPair("FACULTE", "LABORATORY");

        assertThrows(ValidationException.class, () -> types.delete("LABORATORY"));

        types.removeAllowedPair("FACULTE", "LABORATORY");
        types.delete("LABORATORY");

        assertTrue(types.getByCode("LABORATORY").isEmpty());
    }

    @Test
    void deactivatedTypeAcceptsNoNewUnitsButKeepsExistingOnes()
    {
        types.deactivate("FACULTE");

        assertFalse(types.getByCode("FACULTE").orElseThrow().active());
        assertEquals(2, hierarchy.getByType("FACULTE").size());

        assertThrows(ValidationException.class, () -> hierarchy.createUnit(CreateUnitCommand.of(
            "FAC_ARTS", "Faculty of Arts", "FACULTE", "univ-x", Map.of("doyen", "Rabe"))));

        types.activate("FACULTE");
        hierarchy.createUnit(CreateUnitCommand.of(
            "FAC_ARTS", "Faculty of Arts", "FACULTE", "univ-x", Map.of("doyen", "Rabe")));

        assertEquals(3, hierarchy.getByType("FACULTE").size());
    }

    @Test
    void managesAllowedParentChildPairs()
    {
        assertTrue(types.isAllowed("FACULTE", "MENTION"));
        assertFalse(types.isAllowed("MENTION", "FACULTE"));
        assertEquals(List.of("MENTION"), types.getAllowedChildTypes("FACULTE"));
        assertEquals(List.of("FACULTE"), types.getAllowedParentTypes("MENTION"));

        types.addAllowedPair("UNIVERSITE", "MENTION");

        assertTrue(types.isAllowed("UNIVERSITE", "MENTION"));
        assertEquals(5, types.getAllowedPairs().size());
    }

    @Test
    void refusesPairWithUnknownTypeOrDuplicate()
    {
        assertThrows(ValidationException.class, () -> types.addAllowedPair("FACULTE", "GHOST"));
        assertThrows(ValidationException.class, () -> types.addAllowedPair("FACULTE", "MENTION"));
        assertEquals(4, types.getAllowedPairs().size());
    }

    @Test
    void refusesToRemovePairUsedByExistingUnits()
    {
        assertThrows(ValidationException.class, () -> types.removeAllowedPair("FACULTE", "MENTION"));
        assertTrue(types.isAllowed("FACULTE", "MENTION"));
        assertThrows(NotFoundException.class, () -> types.removeAllowedPair("MENTION", "FACULTE"));
    }
}
