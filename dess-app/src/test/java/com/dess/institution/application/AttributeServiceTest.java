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

/**
 * @brief Tests for attribute definitions and custom attribute values.
 *
 * <p>Every test starts from the university example loaded into a temporary file, so the existing
 * configuration, its types and its units act as fixtures.</p>
 */
class AttributeServiceTest
{
    @TempDir
    Path tmp;

    InstitutionModule module;
    AttributeService attributes;
    HierarchyService hierarchy;
    UnitTypeService types;

    @BeforeEach
    void setUp()
    {
        module = TestSupport.openUniversity(tmp);
        attributes = module.attributeService();
        hierarchy = module.hierarchyService();
        types = module.unitTypeService();
    }

    private Path subDir(String name)
    {
        try
        {
            return Files.createDirectories(tmp.resolve(name));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, String> mentionValues(String head)
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("chefMention", head);

        return values;
    }

    // ------------------------------------------------------------------ definitions

    @Test
    void definesTextNumberDateAndEmailAttributesOnABrandNewType()
    {
        types.create(UnitTypeDefinition.of("LABORATORY", "Laboratory"));
        attributes.addDefinition("LABORATORY",
                                 AttributeDefinition.of("manager", "Manager", AttributeType.TEXT, true));
        attributes.addDefinition("LABORATORY",
                                 AttributeDefinition.of("headcount", "Headcount", AttributeType.NUMBER, false));
        attributes.addDefinition("LABORATORY",
                                 AttributeDefinition.of("opening", "Opening", AttributeType.DATE, false));
        attributes.addDefinition("LABORATORY",
                                 AttributeDefinition.of("contact", "Contact", AttributeType.EMAIL, false));

        List<AttributeDefinition> definitions = attributes.getDefinitions("LABORATORY");

        assertEquals(List.of("manager", "headcount", "opening", "contact"),
                     definitions.stream().map(AttributeDefinition::key).toList());
        assertEquals(List.of(AttributeType.TEXT, AttributeType.NUMBER, AttributeType.DATE, AttributeType.EMAIL),
                     definitions.stream().map(AttributeDefinition::type).toList());
    }

    @Test
    void refusesDuplicateAttributeKeyInAType()
    {
        assertThrows(ValidationException.class, () -> attributes.addDefinition(
            "MENTION",
            AttributeDefinition.of("chefMention", "Duplicate", AttributeType.TEXT, false)
        ));

        assertEquals(4, attributes.getDefinitions("MENTION").size());
    }

    @Test
    void refusesInvalidKeyAndInvalidDefaultValue()
    {
        assertThrows(ValidationException.class, () -> attributes.addDefinition(
            "MENTION",
            AttributeDefinition.of("1 invalid key", "X", AttributeType.TEXT, false)
        ));

        assertThrows(ValidationException.class, () -> attributes.addDefinition(
            "MENTION",
            AttributeDefinition.of("capacity", "Capacity", AttributeType.NUMBER, false, "a lot")
        ));

        assertEquals(4, attributes.getDefinitions("MENTION").size());
    }

    @Test
    void addingAnOptionalAttributeToAUsedTypeKeepsExistingUnitsValid()
    {
        attributes.addDefinition("MENTION",
                                 AttributeDefinition.of("phone", "Phone", AttributeType.TEXT, false));

        assertEquals(5, attributes.getDefinitions("MENTION").size());
        assertTrue(module.institutionService().check().valid());
        assertTrue(attributes.getValue("mention-droit", "phone").isEmpty());
    }

    @Test
    void addingARequiredAttributeWithoutDefaultToAUsedTypeIsRefused()
    {
        ValidationException e = assertThrows(ValidationException.class, () -> attributes.addDefinition(
            "MENTION",
            AttributeDefinition.of("budget", "Budget", AttributeType.DECIMAL, true)
        ));

        assertTrue(e.getMessage().contains("budget"));
        assertEquals(4, attributes.getDefinitions("MENTION").size());
        assertTrue(module.institutionService().check().valid());
    }

    @Test
    void addingARequiredAttributeWithDefaultBackfillsExistingUnits()
    {
        attributes.addDefinition("MENTION",
                                 AttributeDefinition.of("open", "Open", AttributeType.BOOLEAN, true, "true"));

        assertEquals(Boolean.TRUE, attributes.getTypedValue("mention-droit", "open").orElseThrow());
        assertEquals(Boolean.TRUE, attributes.getTypedValue("mention-informatique", "open").orElseThrow());
        assertTrue(module.institutionService().check().valid());
    }

    @Test
    void changingTheDataTypeIsRefusedWhenExistingValuesWouldBecomeInvalid()
    {
        // effectifMaximum is 500 in the Computer Science Department: 500 is not a date.
        assertThrows(ValidationException.class, () -> attributes.updateDefinition(
            "MENTION",
            AttributeDefinition.of("effectifMaximum", "Effectif maximum", AttributeType.DATE, false)
        ));

        assertEquals(AttributeType.NUMBER,
                     attributes.getDefinitions("MENTION")
                               .stream()
                               .filter(definition -> definition.key().equals("effectifMaximum"))
                               .findFirst()
                               .orElseThrow()
                               .type());
    }

    @Test
    void relaxingADefinitionIsAllowed()
    {
        attributes.updateDefinition("MENTION",
                                    AttributeDefinition.of("email", "E-mail", AttributeType.TEXT, false));

        assertEquals("E-mail",
                     attributes.getDefinitions("MENTION")
                               .stream()
                               .filter(definition -> definition.key().equals("email"))
                               .findFirst()
                               .orElseThrow()
                               .label());

        assertThrows(NotFoundException.class, () -> attributes.updateDefinition(
            "MENTION",
            AttributeDefinition.of("unknown", "x", AttributeType.TEXT, false)
        ));
    }

    @Test
    void makingAnAttributeRequiredIsRefusedWhenSomeUnitsLackAValue()
    {
        // email is only set for the Computer Science Department: the Law Department would become invalid.
        assertThrows(ValidationException.class, () -> attributes.updateDefinition(
            "MENTION",
            AttributeDefinition.of("email", "E-mail address", AttributeType.EMAIL, true)
        ));
    }

    @Test
    void removesAnUnusedDefinitionButNotOneThatHoldsValues()
    {
        attributes.addDefinition("MENTION",
                                 AttributeDefinition.of("phone", "Phone", AttributeType.TEXT, false));
        attributes.removeDefinition("MENTION", "phone");

        assertEquals(4, attributes.getDefinitions("MENTION").size());
        assertThrows(ValidationException.class, () -> attributes.removeDefinition("MENTION", "chefMention"));
        assertThrows(NotFoundException.class, () -> attributes.removeDefinition("MENTION", "unknown"));
    }

    // ------------------------------------------------------------------ values at creation

    @Test
    void createsAUnitWithItsOwnValuesForTheSameDefinitions()
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("chefMention", "Rakoto Jean");
        values.put("email", "mathematics@university.mg");
        values.put("dateCreation", "2021-09-01");
        values.put("effectifMaximum", "250");

        OrganizationalUnit math = hierarchy.createUnit(CreateUnitCommand.of(
            "DEPT_MATH", "Mathematics Department", "MENTION", "faculte-sciences", values));

        assertEquals("Rakoto Jean", attributes.getValue(math.id(), "chefMention").orElseThrow());
        assertEquals(250L, attributes.getTypedValue(math.id(), "effectifMaximum").orElseThrow());
        assertEquals(LocalDate.of(2021, 9, 1), attributes.getTypedValue(math.id(), "dateCreation").orElseThrow());

        // Values of another unit of the same type are not affected.
        assertEquals("Rabe Marie", attributes.getValue("mention-droit", "chefMention").orElseThrow());
    }

    @Test
    void refusesAnIncompatibleValueOnCreation()
    {
        Map<String, String> values = mentionValues("Rabe");
        values.put("effectifMaximum", "Hello");

        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("DEPT_X", "Department X", "MENTION", "faculte-sciences", values)));

        assertTrue(e.getMessage().contains("Hello"));
        assertTrue(hierarchy.getByCode("DEPT_X").isEmpty());
    }

    @Test
    void refusesAMissingRequiredAttributeOnCreation()
    {
        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("DEPT_X", "Department X", "MENTION", "faculte-sciences", Map.of())));

        assertTrue(e.getMessage().contains("chefMention"));

        // A blank value is equivalent to an absent value.
        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("DEPT_X", "Department X", "MENTION", "faculte-sciences", mentionValues("   "))));
    }

    @Test
    void refusesAnAttributeNotDefinedForTheType()
    {
        Map<String, String> values = mentionValues("Rabe");
        values.put("unknown", "x");

        assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("DEPT_X", "Department X", "MENTION", "faculte-sciences", values)));
    }

    @Test
    void appliesDefaultValuesOnCreation()
    {
        InstitutionModule school = TestSupport.openCopy(subDir("hs"), "valid/high-school.xml");
        OrganizationalUnit serie = school.hierarchyService().createUnit(CreateUnitCommand.of(
            "SERIES_A", "Series A", "SERIE", "enseignement-general", Map.of()));

        assertEquals(Boolean.FALSE, school.attributeService().getTypedValue(serie.id(), "selective").orElseThrow());
    }

    // ------------------------------------------------------------------ existing values

    @Test
    void modifiesAValue()
    {
        AttributeValue value = attributes.setValue("mention-droit", "chefMention", "Randria Paul");

        assertEquals("Randria Paul", value.value());
        assertEquals("Randria Paul", attributes.getValue("mention-droit", "chefMention").orElseThrow());
    }

    @Test
    void definesANewValueOnAnExistingUnit()
    {
        attributes.setValue("mention-droit", "effectifMaximum", "120");

        assertEquals(120L, attributes.getTypedValue("mention-droit", "effectifMaximum").orElseThrow());

        // chefMention and dateCreation already existed: three values in total.
        assertEquals(3, attributes.getValues("mention-droit").size());
    }

    @Test
    void refusesInvalidAndUndefinedValuesWhenSetting()
    {
        assertThrows(ValidationException.class,
                     () -> attributes.setValue("mention-droit", "email", "not-an-email"));
        assertThrows(ValidationException.class, () -> attributes.setValue("mention-droit", "unknown", "x"));
        assertThrows(ValidationException.class, () -> attributes.setValue("mention-droit", "chefMention", "  "));
        assertThrows(NotFoundException.class, () -> attributes.setValue("nope", "chefMention", "x"));
        assertEquals("Rabe Marie", attributes.getValue("mention-droit", "chefMention").orElseThrow());
    }

    @Test
    void removesAnOptionalValueButNotARequiredOne()
    {
        attributes.removeValue("mention-droit", "dateCreation");

        assertTrue(attributes.getValue("mention-droit", "dateCreation").isEmpty());
        assertThrows(ValidationException.class, () -> attributes.removeValue("mention-droit", "chefMention"));
        assertEquals("Rabe Marie", attributes.getValue("mention-droit", "chefMention").orElseThrow());
        assertThrows(NotFoundException.class, () -> attributes.removeValue("mention-droit", "dateCreation"));
    }

    @Test
    void replacingAllValuesThroughUpdateUnitIsValidated()
    {
        hierarchy.updateUnit("mention-droit", new UpdateUnitCommand(null, null, null, mentionValues("New Head")));

        assertEquals("New Head", attributes.getValue("mention-droit", "chefMention").orElseThrow());
        assertTrue(attributes.getValue("mention-droit", "dateCreation").isEmpty());
        assertThrows(ValidationException.class, () -> hierarchy.updateUnit(
            "mention-droit",
            new UpdateUnitCommand(null, null, null, Map.of("email", "a@b.mg"))));
    }

    @Test
    void convertsDecimalValues()
    {
        InstitutionModule custom = TestSupport.openCopy(subDir("custom"), "valid/custom-school.xml");

        assertEquals(new BigDecimal("12500000.50"),
                     custom.attributeService().getTypedValue("pole-cordes", "budgetAnnuel").orElseThrow());
    }

    // ------------------------------------------------------------------ side-effect-free checks

    @Test
    void validatesWithoutPersistingAnything()
    {
        assertTrue(attributes.validateValue("MENTION", "effectifMaximum", "500").valid());
        assertFalse(attributes.validateValue("MENTION", "effectifMaximum", "Hello").valid());
        assertFalse(attributes.validateValue("MENTION", "unknown", "x").valid());

        assertTrue(attributes.validateValues("MENTION", mentionValues("Rabe")).valid());

        ValidationReport missing = attributes.validateValues("MENTION", Map.of("email", "x@y.mg"));

        assertFalse(missing.valid());
        assertTrue(missing.errors().get(0).contains("chefMention"));
        assertEquals(11, hierarchy.getSubtree("univ-x").size());
    }
}
