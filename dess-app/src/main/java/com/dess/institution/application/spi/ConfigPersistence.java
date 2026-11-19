package com.dess.institution.application.spi;

import com.dess.institution.domain.InstitutionConfig;

import java.util.Optional;

/**
 * @brief Persistence port for the institution configuration.
 *
 * <p>The module ships an XML-backed implementation,
 * {@link com.dess.institution.infrastructure.xml.InstitutionConfigRepository}; services depend only
 * on this interface.</p>
 *
 * <p>Contract for {@link #save(InstitutionConfig)}: either the new configuration is fully persisted,
 * or the previous state remains intact and an exception is thrown.</p>
 */
public interface ConfigPersistence
{
    /**
     * Loads the persisted configuration.
     *
     * @return the stored configuration, or an empty optional when none exists yet
     * @throws com.dess.institution.exception.ConfigurationLoadException if the stored data cannot be read
     */
    Optional<InstitutionConfig> load();

    /**
     * Persists a configuration atomically.
     *
     * @param config configuration to store
     * @throws com.dess.institution.exception.ConfigurationSaveException if the configuration cannot be persisted
     */
    void save(InstitutionConfig config);
}
