package com.dess.institution.application;

import com.dess.institution.InstitutionModule;
import com.dess.institution.TestSupport;
import com.dess.institution.domain.OrganizationalUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @brief Simulates a consumer of the module, such as the academic or assessment modules and the web
 * layer.
 *
 * <p>Such a consumer only receives the application interfaces and knows neither the XML format, nor
 * the repository, nor any infrastructure class.</p>
 */
class ModuleIntegrationTest
{
    @TempDir
    Path tmp;

    /**
     * Minimal client code that imports only application interfaces and domain objects.
     */
    static final class AcademicLikeConsumer
    {
        private final HierarchyService hierarchy;
        private final AttributeService attributes;

        AcademicLikeConsumer(HierarchyService hierarchy, AttributeService attributes)
        {
            this.hierarchy = hierarchy;
            this.attributes = attributes;
        }

        List<OrganizationalUnit> unitsBelow(String departmentId)
        {
            return hierarchy.getDescendants(departmentId);
        }

        Optional<OrganizationalUnit> find(String unitId)
        {
            return hierarchy.getById(unitId);
        }

        List<OrganizationalUnit> allOfType(String typeCode)
        {
            return hierarchy.getByType(typeCode);
        }

        String breadcrumb(String unitId)
        {
            return hierarchy.getPath(unitId)
                            .stream()
                            .map(OrganizationalUnit::name)
                            .reduce((a, b) -> a + " > " + b)
                            .orElse("");
        }

        Optional<Object> typedAttribute(String unitId, String key)
        {
            return attributes.getTypedValue(unitId, key);
        }
    }

    @Test
    void otherModulesUseOnlyTheApplicationInterfaces()
    {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        AcademicLikeConsumer academic = new AcademicLikeConsumer(module.hierarchyService(), module.attributeService());

        assertEquals(3, academic.unitsBelow("mention-informatique").size());
        assertEquals("Law Department", academic.find("mention-droit").orElseThrow().name());
        assertEquals(2, academic.allOfType("MENTION").size());
        assertEquals("University X > Faculty of Sciences > Computer Science Department > Software Development > Bachelor 3",
                     academic.breadcrumb("dev-l3"));
        assertEquals(500L, academic.typedAttribute("mention-informatique", "effectifMaximum").orElseThrow());
    }

    @Test
    void anotherModuleCanStoreAUnitIdAsItsOwnForeignKey()
    {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        // The academic module would simply store this stable identifier in its own PostgreSQL table.
        String foreignKey = module.hierarchyService().getByCode("DEV_L3").orElseThrow().id();

        module.hierarchyService().moveUnit("parcours-dev", "mention-droit");
        module.hierarchyService().updateUnit("dev-l3", new UpdateUnitCommand("DEV_L3_BIS", null, null, null));

        assertEquals("dev-l3", foreignKey);
        assertEquals("DEV_L3_BIS", module.hierarchyService().getById(foreignKey).orElseThrow().code());
    }

    @Test
    void returnedStructuresCannotBeUsedToCorruptTheConfiguration()
    {
        InstitutionModule module = TestSupport.openUniversity(tmp);
        List<OrganizationalUnit> roots = module.hierarchyService().getRoots();
        OrganizationalUnit unit = module.hierarchyService().getById("mention-droit").orElseThrow();

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> roots.clear());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                                                      () -> unit.values().clear());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                                                      () -> module.institutionService().getConfig().units().clear());
        assertTrue(module.institutionService().check().valid());
    }
}
