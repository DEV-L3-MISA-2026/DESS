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
 * Point d'assemblage du module : construit la persistance XML, charge la configuration et expose les
 * quatre services applicatifs. Utilisable sans CDI (tests, scripts) ; en production les services sont
 * exposés par injection via {@code com.dess.institution.cdi.InstitutionProducers}.
 */
public final class InstitutionModule {

    private final ConfigurationHolder holder;
    private final InstitutionService institutionService;
    private final UnitTypeService unitTypeService;
    private final HierarchyService hierarchyService;
    private final AttributeService attributeService;

    private InstitutionModule(ConfigurationHolder holder) {
        this.holder = holder;
        this.institutionService = new InstitutionServiceImpl(holder);
        this.unitTypeService = new UnitTypeServiceImpl(holder);
        this.hierarchyService = new HierarchyServiceImpl(holder);
        this.attributeService = new AttributeServiceImpl(holder);
    }

    /** Ouvre le module sur un fichier XML. Si le fichier n'existe pas, le module démarre non initialisé. */
    public static InstitutionModule open(Path xmlFile) {
        return open(new InstitutionConfigRepository(xmlFile));
    }

    public static InstitutionModule open(InstitutionSettings settings) {
        return open(settings.configFile());
    }

    /**
     * Ouvre le module sur une persistance quelconque.
     *
     * @throws com.dess.institution.exception.ConfigurationLoadException si le fichier existe mais est
     *                                                                   invalide (jamais écrasé)
     * @throws com.dess.institution.exception.ValidationException        si la configuration viole une règle métier
     */
    public static InstitutionModule open(ConfigPersistence persistence) {
        ConfigurationHolder holder = new ConfigurationHolder(persistence);
        holder.load();
        return new InstitutionModule(holder);
    }

    public InstitutionService institutionService() {
        return institutionService;
    }

    public UnitTypeService unitTypeService() {
        return unitTypeService;
    }

    public HierarchyService hierarchyService() {
        return hierarchyService;
    }

    public AttributeService attributeService() {
        return attributeService;
    }
}
