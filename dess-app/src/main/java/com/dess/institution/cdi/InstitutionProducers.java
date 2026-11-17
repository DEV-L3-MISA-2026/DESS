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
 * Expose les services du module par CDI. Les autres modules et la couche web (servlets, beans CDI)
 * injectent simplement les interfaces :
 * <pre>{@code
 * @Inject HierarchyService hierarchyService;
 * }</pre>
 * Le module n'est ouvert qu'une seule fois (état unique en mémoire) ; le chemin du fichier XML vient de
 * {@link InstitutionSettings#fromEnvironment()}. Si le fichier est invalide, l'injection échoue avec
 * l'exception de chargement et le fichier n'est jamais modifié.
 */
@ApplicationScoped
public class InstitutionProducers {

    private InstitutionModule module;

    private synchronized InstitutionModule module() {
        if (module == null) {
            module = InstitutionModule.open(InstitutionSettings.fromEnvironment());
        }
        return module;
    }

    @Produces
    @ApplicationScoped
    public InstitutionService institutionService() {
        return module().institutionService();
    }

    @Produces
    @ApplicationScoped
    public UnitTypeService unitTypeService() {
        return module().unitTypeService();
    }

    @Produces
    @ApplicationScoped
    public HierarchyService hierarchyService() {
        return module().hierarchyService();
    }

    @Produces
    @ApplicationScoped
    public AttributeService attributeService() {
        return module().attributeService();
    }
}
