package com.dess.institution.application.spi;

import com.dess.institution.domain.InstitutionConfig;

import java.util.Optional;

/**
 * Port de persistance de la configuration. L'implémentation fournie est basée sur un fichier XML
 * ({@code InstitutionConfigRepository}) ; les services ne connaissent que cette interface.
 * <p>
 * Contrat de {@link #save} : soit la nouvelle configuration est entièrement persistée, soit
 * l'état précédent reste intact et une exception est levée.
 */
public interface ConfigPersistence {

    /** @return la configuration persistée, ou vide si aucune n'existe encore */
    Optional<InstitutionConfig> load();

    void save(InstitutionConfig config);
}
