package com.dess.institution.cdi;

import com.dess.institution.InstitutionModule;
import com.dess.institution.InstitutionSettings;
import com.dess.institution.application.AttributeService;
import com.dess.institution.application.HierarchyService;
import com.dess.institution.application.InstitutionService;
import com.dess.institution.application.UnitTypeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * @brief CDI producer of the institution module services.
 *
 * <p>Other modules and the web layer (servlets, CDI beans) inject the interfaces directly:</p>
 *
 * <pre>{@code
 * @Inject
 * private HierarchyService hierarchyService;
 * }</pre>
 *
 * <p>The module is opened only once, so a single in-memory state is shared by every consumer. The
 * XML file path comes from {@link InstitutionSettings#fromEnvironment()}. When the file is invalid,
 * injection fails with the corresponding loading exception and the file is never modified.</p>
 */
@ApplicationScoped
public class InstitutionProducers
{
    private InstitutionModule module;

    /**
     * Returns the lazily opened module, opening it on first use.
     *
     * @return the shared institution module
     */
    private synchronized InstitutionModule module()
    {
        if (module == null)
        {
            module = InstitutionModule.open(InstitutionSettings.fromEnvironment());
        }

        return module;
    }

    /**
     * Produces the institution service.
     *
     * @return the shared institution service
     */
    @Produces
    @ApplicationScoped
    public InstitutionService institutionService()
    {
        return module().institutionService();
    }

    /**
     * Produces the unit type service.
     *
     * @return the shared unit type service
     */
    @Produces
    @ApplicationScoped
    public UnitTypeService unitTypeService()
    {
        return module().unitTypeService();
    }

    /**
     * Produces the hierarchy service.
     *
     * @return the shared hierarchy service
     */
    @Produces
    @ApplicationScoped
    public HierarchyService hierarchyService()
    {
        return module().hierarchyService();
    }

    /**
     * Produces the attribute service.
     *
     * @return the shared attribute service
     */
    @Produces
    @ApplicationScoped
    public AttributeService attributeService()
    {
        return module().attributeService();
    }
}
