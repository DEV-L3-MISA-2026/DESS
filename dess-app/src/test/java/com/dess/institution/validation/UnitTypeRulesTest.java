package com.dess.institution.validation;

import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief Domain rules for unit types: a unique key and a required label (issue #133).
 *
 * <p>The tests build the configuration in memory, so they check the rule itself and not the XML
 * loader or the service.</p>
 */
class UnitTypeRulesTest
{
    private static InstitutionConfig withTypes(UnitTypeDefinition... types)
    {
        return new InstitutionConfig("i1", "TEST", "Test", null, List.of(types), List.of(), List.of());
    }

    @Test
    void acceptsDistinctKeysWithLabels()
    {
        InstitutionConfig config = withTypes(UnitTypeDefinition.of("FACULTY", "Faculty"),
                                             UnitTypeDefinition.of("DEPARTMENT", "Department"));

        assertEquals(List.of(), InstitutionValidator.validate(config));
        assertDoesNotThrow(() -> InstitutionValidator.validateOrThrow(config));
    }

    @Test
    void rejectsDuplicateKeyAndNamesIt()
    {
        InstitutionConfig config = withTypes(UnitTypeDefinition.of("FACULTY", "Faculty"),
                                             UnitTypeDefinition.of("FACULTY", "Another faculty"));

        List<String> errors = InstitutionValidator.validate(config);

        assertEquals(List.of("Type 'FACULTY': duplicate type key"), errors);
    }

    @Test
    void rejectsKeyThatDiffersOnlyByCase()
    {
        InstitutionConfig config = withTypes(UnitTypeDefinition.of("FACULTY", "Faculty"),
                                             UnitTypeDefinition.of("Faculty", "Faculty again"));

        List<String> errors = InstitutionValidator.validate(config);

        assertEquals(List.of("Type 'Faculty': duplicate type key (same key as 'FACULTY')"), errors);
    }

    @Test
    void rejectsBlankLabel()
    {
        InstitutionConfig config = withTypes(UnitTypeDefinition.of("FACULTY", "   "));

        ValidationException e = assertThrows(ValidationException.class,
                                             () -> InstitutionValidator.validateOrThrow(config));

        assertEquals(List.of("Type 'FACULTY': label required"), e.getViolations());
    }

    @Test
    void rejectsMissingLabel()
    {
        InstitutionConfig config = withTypes(UnitTypeDefinition.of("FACULTY", null));

        assertTrue(InstitutionValidator.validate(config).contains("Type 'FACULTY': label required"));
    }

    @Test
    void reportsDuplicateKeyAndBlankLabelTogether()
    {
        InstitutionConfig config = withTypes(UnitTypeDefinition.of("FACULTY", "Faculty"),
                                             UnitTypeDefinition.of("FACULTY", ""));

        List<String> errors = InstitutionValidator.validate(config);

        assertEquals(2, errors.size(), errors.toString());
        assertTrue(errors.contains("Type 'FACULTY': duplicate type key"));
        assertTrue(errors.contains("Type 'FACULTY': label required"));
    }
}
