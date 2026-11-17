package com.dess.institution.infrastructure.xml;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.MalformedXmlException;
import com.dess.institution.exception.SchemaViolationException;
import com.dess.institution.exception.ValidationException;
import com.dess.institution.validation.InstitutionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlConfigLoaderTest {

    @TempDir
    Path tmp;

    private final XmlConfigLoader loader = new XmlConfigLoader();

    private InstitutionConfig load(String resource) {
        return loader.load(TestSupport.resource(resource));
    }

    // ---------------------------------------------------------------- chargements valides

    @Test
    void loadsUniversity() {
        InstitutionConfig cfg = load("valid/university.xml");
        assertEquals("UNIVERSITE_X", cfg.code());
        assertEquals(5, cfg.unitTypes().size());
        assertEquals(4, cfg.allowedPairs().size());
        assertEquals(11, cfg.units().size());
        assertTrue(InstitutionValidator.validate(cfg).isEmpty());

        OrganizationalUnit mention = cfg.findUnit("mention-informatique").orElseThrow();
        assertEquals("faculte-sciences", mention.parentId());
        assertEquals("500", mention.value("effectifMaximum").orElseThrow().value());
        assertEquals("2020-01-15", mention.value("dateCreation").orElseThrow().value());
    }

    @Test
    void loadsHighSchool() {
        InstitutionConfig cfg = load("valid/high-school.xml");
        assertEquals("LYCEE_Y", cfg.code());
        assertTrue(cfg.findType("SERIE").isPresent());
        assertTrue(cfg.findType("FACULTE").isEmpty());
        assertTrue(InstitutionValidator.validate(cfg).isEmpty());
        assertEquals("serie-c", cfg.findUnitByCode("SERIE_C").orElseThrow().id());
    }

    @Test
    void loadsCustomSchoolWithTypesInventedForTheOccasion() {
        InstitutionConfig cfg = load("valid/custom-school.xml");
        Set<String> typeCodes = cfg.unitTypes().stream().map(UnitTypeDefinition::code).collect(Collectors.toSet());
        assertTrue(typeCodes.contains("CLASSE_INSTRUMENT"));
        assertTrue(typeCodes.contains("ANNEXE"));
        assertTrue(InstitutionValidator.validate(cfg).isEmpty());
        assertEquals(2L, cfg.units().stream().filter(OrganizationalUnit::isRoot).count());
        assertFalse(cfg.findUnit("pole-vents").orElseThrow().active());
    }

    /** Les trois exemples passent par exactement le même code Java, sans aucune adaptation. */
    @ParameterizedTest
    @ValueSource(strings = {"university", "high-school", "custom-school"})
    void sameJavaCodeLoadsEveryExample(String name) {
        InstitutionModule module = TestSupport.openCopy(tmp, "valid/" + name + ".xml");
        assertTrue(module.institutionService().check().valid());
        assertFalse(module.hierarchyService().getRoots().isEmpty());
        assertFalse(module.unitTypeService().getAll().isEmpty());
    }

    @Test
    void packagedExamplesAreIdenticalToTestFixturesAndLoad() throws IOException {
        for (String name : List.of("university", "high-school", "custom-school")) {
            Path packaged = TestSupport.resource("examples/" + name + ".xml");
            assertArrayEquals(Files.readAllBytes(TestSupport.resource("valid/" + name + ".xml")),
                    Files.readAllBytes(packaged));
            assertTrue(InstitutionValidator.validate(loader.load(packaged)).isEmpty());
        }
    }

    // ---------------------------------------------------------------- rejets

    @Test
    void rejectsMalformedXml() {
        assertThrows(MalformedXmlException.class, () -> load("invalid/malformed.xml"));
    }

    @Test
    void rejectsXmlThatViolatesTheXsd() {
        SchemaViolationException e = assertThrows(SchemaViolationException.class,
                () -> load("invalid/schema-violation.xml"));
        assertFalse(e.getViolations().isEmpty());
    }

    @Test
    void rejectsDuplicateValueKeyThroughXsdUniqueConstraint() {
        assertThrows(SchemaViolationException.class, () -> load("invalid/duplicate-value-key.xml"));
    }

    @Test
    void rejectsDoctypeSoExternalEntitiesAreNeverResolved() {
        MalformedXmlException e = assertThrows(MalformedXmlException.class, () -> load("invalid/xxe.xml"));
        assertFalse(e.getMessage().contains("root:"), "le contenu d'un fichier système ne doit jamais fuiter");
    }

    @Test
    void unknownUnitTypeIsRejectedBySemanticValidation() {
        InstitutionConfig cfg = load("invalid/unknown-unit-type.xml"); // structure valide
        List<String> errors = InstitutionValidator.validate(cfg);
        assertTrue(errors.stream().anyMatch(m -> m.contains("GHOST")), errors.toString());

        Path file = TestSupport.copyTo(tmp, "invalid/unknown-unit-type.xml");
        assertThrows(ValidationException.class, () -> InstitutionModule.open(file));
    }

    @Test
    void forbiddenParentChildIsRejected() {
        List<String> errors = InstitutionValidator.validate(load("invalid/forbidden-parent-child.xml"));
        assertTrue(errors.stream().anyMatch(m -> m.contains("relation interdite")), errors.toString());
    }

    @Test
    void duplicateUnitCodeIsRejected() {
        List<String> errors = InstitutionValidator.validate(load("invalid/duplicate-code.xml"));
        assertTrue(errors.stream().anyMatch(m -> m.contains("dupliqué")), errors.toString());
    }

    @Test
    void invalidAttributeValueIsRejected() {
        List<String> errors = InstitutionValidator.validate(load("invalid/invalid-attribute-value.xml"));
        assertTrue(errors.stream().anyMatch(m -> m.contains("Bonjour")), errors.toString());
    }

    @Test
    void missingRequiredAttributeIsRejected() {
        List<String> errors = InstitutionValidator.validate(load("invalid/missing-required-attribute.xml"));
        assertTrue(errors.stream().anyMatch(m -> m.contains("obligatoire") && m.contains("chef")), errors.toString());
    }

    @Test
    void invalidFileIsNeverOverwrittenAtStartup() throws IOException {
        Path file = TestSupport.copyTo(tmp, "invalid/malformed.xml");
        byte[] before = Files.readAllBytes(file);
        assertThrows(MalformedXmlException.class, () -> InstitutionModule.open(file));
        assertArrayEquals(before, Files.readAllBytes(file));
    }
}
