package com.dess.institution;

import com.dess.institution.application.AttributeService;
import com.dess.institution.application.HierarchyService;
import com.dess.institution.application.InstitutionService;
import com.dess.institution.application.UnitTypeService;
import com.dess.institution.application.impl.AttributeServiceImpl;
import com.dess.institution.application.impl.ConfigurationHolder;
import com.dess.institution.application.impl.HierarchyServiceImpl;
import com.dess.institution.application.impl.InstitutionServiceImpl;
import com.dess.institution.application.impl.UnitTypeServiceImpl;
import com.dess.institution.application.spi.ConfigPersistence;
import com.dess.institution.infrastructure.xml.InstitutionConfigRepository;

import java.nio.file.Path;

/**
 * @brief Composition root of the institution module.
 *
 * <p>The module builds the XML persistence, loads the configuration and exposes the four
 * application services. It can be used without CDI (tests, scripts); in a Jakarta EE runtime the
 * services are produced by {@link com.dess.institution.cdi.InstitutionProducers}.</p>
 *
 * @see InstitutionSettings
 */
public final class InstitutionModule
{
    private final InstitutionService institutionService;
    private final UnitTypeService unitTypeService;
    private final HierarchyService hierarchyService;
    private final AttributeService attributeService;

    private InstitutionModule(ConfigurationHolder holder)
    {
        this.institutionService = new InstitutionServiceImpl(holder);
        this.unitTypeService = new UnitTypeServiceImpl(holder);
        this.hierarchyService = new HierarchyServiceImpl(holder);
        this.attributeService = new AttributeServiceImpl(holder);
    }

    /**
     * Opens the module on an XML file. When the file does not exist yet, the module starts in the
     * not-initialized state.
     *
     * @param xmlFile configuration file path
     * @return an initialized module
     * @throws com.dess.institution.exception.ConfigurationLoadException if the file exists but cannot be read
     * @throws com.dess.institution.exception.ValidationException if the stored configuration is invalid
     */
    public static InstitutionModule open(Path xmlFile)
    {
        return open(new InstitutionConfigRepository(xmlFile));
    }

    /**
     * Opens the module using the file configured in the supplied settings.
     *
     * @param settings module settings
     * @return an initialized module
     * @throws com.dess.institution.exception.ConfigurationLoadException if the file exists but cannot be read
     * @throws com.dess.institution.exception.ValidationException if the stored configuration is invalid
     */
    public static InstitutionModule open(InstitutionSettings settings)
    {
        return open(settings.configFile());
    }

    /**
     * Opens the module on an arbitrary persistence implementation.
     *
     * @param persistence persistence port to use
     * @return an initialized module
     * @throws com.dess.institution.exception.ConfigurationLoadException if the stored configuration cannot be read;
     *                                                                   the existing data is never overwritten
     * @throws com.dess.institution.exception.ValidationException if the stored configuration violates a business rule
     */
    public static InstitutionModule open(ConfigPersistence persistence)
    {
        ConfigurationHolder holder = new ConfigurationHolder(persistence);
        holder.load();

        return new InstitutionModule(holder);
    }

    /**
     * Returns the institution service.
     *
     * @return the institution information and configuration lifecycle service
     */
    public InstitutionService institutionService()
    {
        return institutionService;
    }

    /**
     * Returns the unit type service.
     *
     * @return the unit type and allowed relation service
     */
    public UnitTypeService unitTypeService()
    {
        return unitTypeService;
    }

    /**
     * Returns the hierarchy service.
     *
     * @return the organizational unit tree service
     */
    public HierarchyService hierarchyService()
    {
        return hierarchyService;
    }

    /**
     * Returns the attribute service.
     *
     * @return the custom attribute definition and value service
     */
    public AttributeService attributeService()
    {
        return attributeService;
    }
}
