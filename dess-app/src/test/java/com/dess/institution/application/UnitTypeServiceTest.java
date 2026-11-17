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

class UnitTypeServiceTest {

    @TempDir
    Path tmp;

    UnitTypeService types;
    HierarchyService hierarchy;

    @BeforeEach
    void setUp() {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        types = module.unitTypeService();
        hierarchy = module.hierarchyService();
    }

    @Test
    void createsATypeUnknownInAdvance() {
        UnitTypeDefinition created = types.create(UnitTypeDefinition.of("LABORATOIRE", "Laboratoire"));
        assertEquals("LABORATOIRE", created.code());
        assertTrue(created.active());
        assertEquals(6, types.getAll().size());
    }

    @Test
    void getsByCode() {
        assertEquals("Mention", types.getByCode("MENTION").orElseThrow().label());
        assertTrue(types.getByCode("INEXISTANT").isEmpty());
    }

    @Test
    void updatesLabelAndDescriptionButNeverTheCode() {
        UnitTypeDefinition updated = types.update("MENTION", "Mention d'études", "Domaine d'études");
        assertEquals("MENTION", updated.code());
        assertEquals("Mention d'études", types.getByCode("MENTION").orElseThrow().label());
        assertEquals("Domaine d'études", types.getByCode("MENTION").orElseThrow().description());
        // les définitions d'attributs sont conservées
        assertEquals(4, types.getByCode("MENTION").orElseThrow().attributes().size());
    }

    @Test
    void updateOfUnknownTypeFails() {
        assertThrows(NotFoundException.class, () -> types.update("NOPE", "x", null));
    }

    @Test
    void refusesDuplicateTypeCode() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> types.create(UnitTypeDefinition.of("MENTION", "Autre mention")));
        assertTrue(e.getMessage().contains("MENTION"));
        assertEquals(5, types.getAll().size());
    }

    @Test
    void refusesInvalidTypeCode() {
        assertThrows(ValidationException.class, () -> types.create(UnitTypeDefinition.of("mauvais code", "x")));
        assertThrows(ValidationException.class, () -> types.create(UnitTypeDefinition.of("", "x")));
    }

    @Test
    void deletesAnUnusedType() {
        types.create(UnitTypeDefinition.of("LABORATOIRE", "Laboratoire"));
        types.delete("LABORATOIRE");
        assertTrue(types.getByCode("LABORATOIRE").isEmpty());
    }

    @Test
    void refusesToDeleteATypeStillUsedByUnits() {
        ValidationException e = assertThrows(ValidationException.class, () -> types.delete("MENTION"));
        assertTrue(e.getMessage().contains("utilisé"));
        assertTrue(types.getByCode("MENTION").isPresent());
        // aucune unité ne référence un type inexistant
        assertEquals(2, hierarchy.getByType("MENTION").size());
    }

    @Test
    void refusesToDeleteATypeReferencedByAllowedPairs() {
        types.create(UnitTypeDefinition.of("LABORATOIRE", "Laboratoire"));
        types.addAllowedPair("FACULTE", "LABORATOIRE");
        assertThrows(ValidationException.class, () -> types.delete("LABORATOIRE"));
        types.removeAllowedPair("FACULTE", "LABORATOIRE");
        types.delete("LABORATOIRE");
        assertTrue(types.getByCode("LABORATOIRE").isEmpty());
    }

    @Test
    void deactivatedTypeAcceptsNoNewUnitsButKeepsExistingOnes() {
        types.deactivate("FACULTE");
        assertFalse(types.getByCode("FACULTE").orElseThrow().active());
        assertEquals(2, hierarchy.getByType("FACULTE").size());

        assertThrows(ValidationException.class, () -> hierarchy.createUnit(CreateUnitCommand.of(
                "FAC_LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "Rabe"))));

        types.activate("FACULTE");
        hierarchy.createUnit(CreateUnitCommand.of(
                "FAC_LETTRES", "Faculté des Lettres", "FACULTE", "univ-x", Map.of("doyen", "Rabe")));
        assertEquals(3, hierarchy.getByType("FACULTE").size());
    }

    @Test
    void managesAllowedParentChildPairs() {
        assertTrue(types.isAllowed("FACULTE", "MENTION"));
        assertFalse(types.isAllowed("MENTION", "FACULTE"));
        assertEquals(List.of("MENTION"), types.getAllowedChildTypes("FACULTE"));
        assertEquals(List.of("FACULTE"), types.getAllowedParentTypes("MENTION"));

        types.addAllowedPair("UNIVERSITE", "MENTION");
        assertTrue(types.isAllowed("UNIVERSITE", "MENTION"));
        assertEquals(5, types.getAllowedPairs().size());
    }

    @Test
    void refusesPairWithUnknownTypeOrDuplicate() {
        assertThrows(ValidationException.class, () -> types.addAllowedPair("FACULTE", "GHOST"));
        assertThrows(ValidationException.class, () -> types.addAllowedPair("FACULTE", "MENTION"));
        assertEquals(4, types.getAllowedPairs().size());
    }

    @Test
    void refusesToRemovePairUsedByExistingUnits() {
        assertThrows(ValidationException.class, () -> types.removeAllowedPair("FACULTE", "MENTION"));
        assertTrue(types.isAllowed("FACULTE", "MENTION"));
        assertThrows(NotFoundException.class, () -> types.removeAllowedPair("MENTION", "FACULTE"));
    }
}
