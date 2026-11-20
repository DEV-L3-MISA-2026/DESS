package com.dess.institution.application;

import com.dess.institution.domain.InstitutionConfig;

/**
 * @brief Provides the institution information and the lifecycle of its configuration.
 *
 * <p>This interface is the public entry point of the module. Other modules and the web layer
 * (servlets or CDI beans) use these interfaces only; they never depend on the XML format or on the
 * infrastructure classes.</p>
 *
 * @see HierarchyService
 * @see UnitTypeService
 * @see AttributeService
 */
public interface InstitutionService
{
    /**
     * Returns the current configuration lifecycle state.
     *
     * @return {@link ConfigurationStatus#READY} when a configuration is loaded
     */
    ConfigurationStatus getStatus();

    /**
     * Explicitly creates the institution, without unit types or organizational units, when no
     * configuration exists yet.
     *
     * @param code        business code
     * @param name        display name
     * @param description optional description
     * @return the created institution information
     * @throws com.dess.institution.exception.ValidationException if the institution is already initialized or
     *                                                           the supplied values are invalid
     */
    InstitutionInfo initialize(String code, String name, String description);

    /**
     * Returns the complete configuration.
     *
     * @return an immutable snapshot of the configuration
     * @throws com.dess.institution.exception.NotInitializedException if no configuration is loaded
     */
    InstitutionConfig getConfig();

    /**
     * Returns the general institution information.
     *
     * @return the institution information
     * @throws com.dess.institution.exception.NotInitializedException if no configuration is loaded
     */
    InstitutionInfo getInfo();

    /**
     * Updates the code, name and description of the institution. The identifier never changes.
     *
     * @param code        new business code
     * @param name        new display name
     * @param description new optional description
     * @return the updated institution information
     * @throws com.dess.institution.exception.NotInitializedException if no configuration is loaded
     * @throws com.dess.institution.exception.ValidationException if the resulting configuration is invalid
     */
    InstitutionInfo updateInfo(String code, String name, String description);

    /**
     * Revalidates the complete in-memory configuration without modifying it.
     *
     * @return the validation report
     * @throws com.dess.institution.exception.NotInitializedException if no configuration is loaded
     */
    ValidationReport check();

    /**
     * Reloads the configuration from the XML file, for example after a manual edit.
     *
     * @throws com.dess.institution.exception.ConfigurationLoadException if the file cannot be read or parsed
     * @throws com.dess.institution.exception.ValidationException if the loaded configuration is invalid
     */
    void reload();
}
