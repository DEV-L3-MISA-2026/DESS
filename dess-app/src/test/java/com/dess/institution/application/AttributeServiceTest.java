package com.dess.institution.application;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeType;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.NotFoundException;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeServiceTest {

    @TempDir
    Path tmp;

    InstitutionModule module;
    AttributeService attributes;
    HierarchyService hierarchy;
    UnitTypeService types;

    @BeforeEach
    void setUp() {
        module = TestSupport.openUniversity(tmp);
        attributes = module.attributeService();
        hierarchy = module.hierarchyService();
        types = module.unitTypeService();
    }

    private Path subDir(String name) {
        try {
            return Files.createDirectories(tmp.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, String> mentionValues(String chef) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("chefMention", chef);
        return values;
    }

    // ---------------------------------------------------------------- définitions

    @Test
    void definesTextNumberDateAndEmailAttributesOnABrandNewType() {
        types.create(UnitTypeDefinition.of("LABORATOIRE", "Laboratoire"));
        attributes.addDefinition("LABORATOIRE", AttributeDefinition.of("responsable", "Responsable", AttributeType.TEXT, true));
        attributes.addDefinition("LABORATOIRE", AttributeDefinition.of("effectif", "Effectif", AttributeType.NUMBER, false));
        attributes.addDefinition("LABORATOIRE", AttributeDefinition.of("ouverture", "Ouverture", AttributeType.DATE, false));
        attributes.addDefinition("LABORATOIRE", AttributeDefinition.of("contact", "Contact", AttributeType.EMAIL, false));

        List<AttributeDefinition> defs = attributes.getDefinitions("LABORATOIRE");
        assertEquals(List.of("responsable", "effectif", "ouverture", "contact"),
                defs.stream().map(AttributeDefinition::key).toList());
        assertEquals(List.of(AttributeType.TEXT, AttributeType.NUMBER, AttributeType.DATE, AttributeType.EMAIL),
                defs.stream().map(AttributeDefinition::type).toList());
    }

    @Test
    void refusesDuplicateAttributeKeyInAType() {
        assertThrows(ValidationException.class, () -> attributes.addDefinition("MENTION",
                AttributeDefinition.of("chefMention", "Doublon", AttributeType.TEXT, false)));
        assertEquals(4, attributes.getDefinitions("MENTION").size());
    }

    @Test
    void refusesInvalidKeyAndInvalidDefaultValue() {
        assertThrows(ValidationException.class, () -> attributes.addDefinition("MENTION",
                AttributeDefinition.of("1 clé invalide", "X", AttributeType.TEXT, false)));
        assertThrows(ValidationException.class, () -> attributes.addDefinition("MENTION",
                AttributeDefinition.of("capacite", "Capacité", AttributeType.NUMBER, false, "beaucoup")));
        assertEquals(4, attributes.getDefinitions("MENTION").size());
    }

    @Test
    void addingAnOptionalAttributeToAUsedTypeKeepsExistingUnitsValid() {
        attributes.addDefinition("MENTION", AttributeDefinition.of("telephone", "Téléphone", AttributeType.TEXT, false));
        assertEquals(5, attributes.getDefinitions("MENTION").size());
        assertTrue(module.institutionService().check().valid());
        assertTrue(attributes.getValue("mention-droit", "telephone").isEmpty());
    }

    @Test
    void addingARequiredAttributeWithoutDefaultToAUsedTypeIsRefused() {
        ValidationException e = assertThrows(ValidationException.class, () -> attributes.addDefinition("MENTION",
                AttributeDefinition.of("budget", "Budget", AttributeType.DECIMAL, true)));
        assertTrue(e.getMessage().contains("budget"));
        assertEquals(4, attributes.getDefinitions("MENTION").size());
        assertTrue(module.institutionService().check().valid());
    }

    @Test
    void addingARequiredAttributeWithDefaultBackfillsExistingUnits() {
        attributes.addDefinition("MENTION",
                AttributeDefinition.of("ouverte", "Ouverte", AttributeType.BOOLEAN, true, "true"));
        assertEquals(Boolean.TRUE, attributes.getTypedValue("mention-droit", "ouverte").orElseThrow());
        assertEquals(Boolean.TRUE, attributes.getTypedValue("mention-informatique", "ouverte").orElseThrow());
        assertTrue(module.institutionService().check().valid());
    }

    @Test
    void changingTheDataTypeIsRefusedWhenExistingValuesWouldBecomeInvalid() {
        // effectifMaximum vaut 500 dans Mention Informatique : 500 n'est pas une date
        assertThrows(ValidationException.class, () -> attributes.updateDefinition("MENTION",
                AttributeDefinition.of("effectifMaximum", "Effectif maximum", AttributeType.DATE, false)));
        assertEquals(AttributeType.NUMBER, attributes.getDefinitions("MENTION").stream()
                .filter(d -> d.key().equals("effectifMaximum")).findFirst().orElseThrow().type());
    }

    @Test
    void relaxingADefinitionIsAllowed() {
        attributes.updateDefinition("MENTION",
                AttributeDefinition.of("email", "Courriel", AttributeType.TEXT, false));
        assertEquals("Courriel", attributes.getDefinitions("MENTION").stream()
                .filter(d -> d.key().equals("email")).findFirst().orElseThrow().label());
        assertThrows(NotFoundException.class, () -> attributes.updateDefinition("MENTION",
                AttributeDefinition.of("inconnu", "x", AttributeType.TEXT, false)));
    }

    @Test
    void makingAnAttributeRequiredIsRefusedWhenSomeUnitsLackAValue() {
        // « email » n'est renseigné que pour Mention Informatique : Mention Droit deviendrait invalide
        assertThrows(ValidationException.class, () -> attributes.updateDefinition("MENTION",
                AttributeDefinition.of("email", "Adresse e-mail", AttributeType.EMAIL, true)));
    }

    @Test
    void removesAnUnusedDefinitionButNotOneThatHoldsValues() {
        attributes.addDefinition("MENTION", AttributeDefinition.of("telephone", "Téléphone", AttributeType.TEXT, false));
        attributes.removeDefinition("MENTION", "telephone");
        assertEquals(4, attributes.getDefinitions("MENTION").size());

        assertThrows(ValidationException.class, () -> attributes.removeDefinition("MENTION", "chefMention"));
        assertThrows(NotFoundException.class, () -> attributes.removeDefinition("MENTION", "inconnu"));
    }

    // ---------------------------------------------------------------- valeurs à la création

    @Test
    void createsAUnitWithItsOwnValuesForTheSameDefinitions() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("chefMention", "Rakoto Jean");
        values.put("email", "mathematiques@universite.mg");
        values.put("dateCreation", "2021-09-01");
        values.put("effectifMaximum", "250");
        OrganizationalUnit math = hierarchy.createUnit(CreateUnitCommand.of(
                "MENTION_MATHS", "Mention Mathématiques", "MENTION", "faculte-sciences", values));

        assertEquals("Rakoto Jean", attributes.getValue(math.id(), "chefMention").orElseThrow());
        assertEquals(250L, attributes.getTypedValue(math.id(), "effectifMaximum").orElseThrow());
        assertEquals(LocalDate.of(2021, 9, 1), attributes.getTypedValue(math.id(), "dateCreation").orElseThrow());
        // les valeurs d'une autre unité du même type ne sont pas affectées
        assertEquals("Rabe Marie", attributes.getValue("mention-droit", "chefMention").orElseThrow());
    }

    @Test
    void refusesAnIncompatibleValueOnCreation() {
        Map<String, String> values = mentionValues("Rabe");
        values.put("effectifMaximum", "Bonjour");
        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("MENTION_X", "Mention X", "MENTION", "faculte-sciences", values)));
        assertTrue(e.getMessage().contains("Bonjour"));
        assertTrue(hierarchy.getByCode("MENTION_X").isEmpty());
    }

    @Test
    void refusesAMissingRequiredAttributeOnCreation() {
        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("MENTION_X", "Mention X", "MENTION", "faculte-sciences", Map.of())));
        assertTrue(e.getMessage().contains("chefMention"));
        // une valeur blanche équivaut à une valeur absente
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("MENTION_X", "Mention X", "MENTION", "faculte-sciences", mentionValues("   "))));
    }

    @Test
    void refusesAnAttributeNotDefinedForTheType() {
        Map<String, String> values = mentionValues("Rabe");
        values.put("inconnu", "x");
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
                CreateUnitCommand.of("MENTION_X", "Mention X", "MENTION", "faculte-sciences", values)));
    }

    @Test
    void appliesDefaultValuesOnCreation() {
        InstitutionModule school = TestSupport.openCopy(subDir("hs"), "valid/high-school.xml");
        OrganizationalUnit serie = school.hierarchyService().createUnit(CreateUnitCommand.of(
                "SERIE_A", "Série A", "SERIE", "enseignement-general", Map.of()));
        assertEquals(Boolean.FALSE, school.attributeService().getTypedValue(serie.id(), "selective").orElseThrow());
    }

    // ---------------------------------------------------------------- valeurs existantes

    @Test
    void modifiesAValue() {
        AttributeValue v = attributes.setValue("mention-droit", "chefMention", "Randria Paul");
        assertEquals("Randria Paul", v.value());
        assertEquals("Randria Paul", attributes.getValue("mention-droit", "chefMention").orElseThrow());
    }

    @Test
    void definesANewValueOnAnExistingUnit() {
        attributes.setValue("mention-droit", "effectifMaximum", "120");
        assertEquals(120L, attributes.getTypedValue("mention-droit", "effectifMaximum").orElseThrow());
        // chefMention et dateCreation existaient déjà : trois valeurs au total
        assertEquals(3, attributes.getValues("mention-droit").size());
    }

    @Test
    void refusesInvalidAndUndefinedValuesWhenSetting() {
        assertThrows(ValidationException.class, () -> attributes.setValue("mention-droit", "email", "pas-un-email"));
        assertThrows(ValidationException.class, () -> attributes.setValue("mention-droit", "inconnu", "x"));
        assertThrows(ValidationException.class, () -> attributes.setValue("mention-droit", "chefMention", "  "));
        assertThrows(NotFoundException.class, () -> attributes.setValue("nope", "chefMention", "x"));
        assertEquals("Rabe Marie", attributes.getValue("mention-droit", "chefMention").orElseThrow());
    }

    @Test
    void removesAnOptionalValueButNotARequiredOne() {
        attributes.removeValue("mention-droit", "dateCreation");
        assertTrue(attributes.getValue("mention-droit", "dateCreation").isEmpty());
        assertThrows(ValidationException.class, () -> attributes.removeValue("mention-droit", "chefMention"));
        assertEquals("Rabe Marie", attributes.getValue("mention-droit", "chefMention").orElseThrow());
        assertThrows(NotFoundException.class, () -> attributes.removeValue("mention-droit", "dateCreation"));
    }

    @Test
    void replacingAllValuesThroughUpdateUnitIsValidated() {
        hierarchy.updateUnit("mention-droit", new UpdateUnitCommand(null, null, null, mentionValues("Nouveau Chef")));
        assertEquals("Nouveau Chef", attributes.getValue("mention-droit", "chefMention").orElseThrow());
        assertTrue(attributes.getValue("mention-droit", "dateCreation").isEmpty());
        assertThrows(ValidationException.class, () -> hierarchy.updateUnit("mention-droit",
                new UpdateUnitCommand(null, null, null, Map.of("email", "a@b.mg"))));
    }

    @Test
    void convertsDecimalValues() {
        InstitutionModule custom = TestSupport.openCopy(subDir("custom"), "valid/custom-school.xml");
        assertEquals(new BigDecimal("12500000.50"),
                custom.attributeService().getTypedValue("pole-cordes", "budgetAnnuel").orElseThrow());
    }

    // ---------------------------------------------------------------- vérifications sans effet de bord

    @Test
    void validatesWithoutPersistingAnything() {
        assertTrue(attributes.validateValue("MENTION", "effectifMaximum", "500").valid());
        assertFalse(attributes.validateValue("MENTION", "effectifMaximum", "Bonjour").valid());
        assertFalse(attributes.validateValue("MENTION", "inconnu", "x").valid());

        assertTrue(attributes.validateValues("MENTION", mentionValues("Rabe")).valid());
        ValidationReport missing = attributes.validateValues("MENTION", Map.of("email", "x@y.mg"));
        assertFalse(missing.valid());
        assertTrue(missing.errors().get(0).contains("chefMention"));
        assertEquals(11, hierarchy.getSubtree("univ-x").size());
    }
}
