package com.dess.institution.application;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief A unit code is unique among the children of one parent, without case (issue #135).
 *
 * <p>The university fixture has two faculties under {@code univ-x}: {@code faculte-droit} with the
 * department {@code MENTION_DROIT}, and {@code faculte-sciences} with {@code MENTION_INFO}.</p>
 */
class UnitCodeScopeTest
{
    private static final Map<String, String> HEAD = Map.of("chefMention", "Rabe Marie");

    @TempDir
    Path tmp;

    HierarchyService hierarchy;

    @BeforeEach
    void setUp()
    {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        hierarchy = module.hierarchyService();
    }

    private OrganizationalUnit department(String code, String parentId)
    {
        return hierarchy.createUnit(CreateUnitCommand.of(code, "Department " + code, "MENTION", parentId, HEAD));
    }

    @Test
    void acceptsTheSameCodeUnderAnotherParent()
    {
        OrganizationalUnit law = department("HISTORY", "faculte-droit");
        OrganizationalUnit science = department("HISTORY", "faculte-sciences");

        assertEquals("faculte-droit", law.parentId());
        assertEquals("faculte-sciences", science.parentId());
        assertEquals(2, hierarchy.getByType("MENTION").stream()
                                 .filter(unit -> unit.code().equals("HISTORY"))
                                 .count());
    }

    @Test
    void rejectsTheSameCodeUnderTheSameParent()
    {
        department("HISTORY", "faculte-droit");

        ValidationException e = assertThrows(ValidationException.class,
                                             () -> department("HISTORY", "faculte-droit"));

        assertEquals(List.of("Unit 'Department HISTORY' (HISTORY): duplicate code 'HISTORY' under "
                             + "'Faculty of Law' (DROIT), already used by 'Department HISTORY' (HISTORY)"),
                     e.getViolations());
        assertEquals(2, hierarchy.getChildren("faculte-droit").size());
    }

    @Test
    void comparesCodesWithoutCase()
    {
        ValidationException e = assertThrows(ValidationException.class,
                                             () -> department("mention_droit", "faculte-droit"));

        assertEquals(1, e.getViolations().size());
        assertTrue(e.getViolations().get(0).contains("duplicate code 'mention_droit' under 'Faculty of Law' (DROIT)"),
                   e.getViolations().toString());
        assertTrue(e.getViolations().get(0).endsWith("already used by 'Law Department' (MENTION_DROIT)"),
                   e.getViolations().toString());
    }

    @Test
    void rootUnitsShareOneScope()
    {
        ValidationException e = assertThrows(ValidationException.class, () -> hierarchy.createUnit(
            CreateUnitCommand.of("univ_x", "University X bis", "UNIVERSITE", null, null)));

        assertTrue(e.getViolations().get(0).contains("among the root units"), e.getViolations().toString());
        assertEquals(1, hierarchy.getRoots().size());
    }

    @Test
    void renamingToASiblingCodeIsRejected()
    {
        OrganizationalUnit history = department("HISTORY", "faculte-droit");

        assertThrows(ValidationException.class, () -> hierarchy.updateUnit(
            history.id(), new UpdateUnitCommand("MENTION_DROIT", null, null, null)));

        assertEquals("HISTORY", hierarchy.getById(history.id()).orElseThrow().code());
    }

    @Test
    void movingIntoAParentThatAlreadyHasTheCodeIsRejected()
    {
        OrganizationalUnit history = department("MENTION_INFO", "faculte-droit");

        ValidationException e = assertThrows(ValidationException.class,
                                             () -> hierarchy.moveUnit(history.id(), "faculte-sciences"));

        assertTrue(e.getMessage().contains("duplicate code 'MENTION_INFO' under 'Faculty of Sciences'"),
                   e.getMessage());
        assertEquals("faculte-droit", hierarchy.getById(history.id()).orElseThrow().parentId());
    }
}
