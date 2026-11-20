package com.dess.institution.application;

/**
 * @brief Lifecycle state of the institution configuration.
 */
public enum ConfigurationStatus
{
    /**
     * No configuration is loaded. An initial XML file or an explicit initialization is required.
     */
    NOT_INITIALIZED,

    /**
     * A valid configuration is loaded and ready to use.
     */
    READY
}
