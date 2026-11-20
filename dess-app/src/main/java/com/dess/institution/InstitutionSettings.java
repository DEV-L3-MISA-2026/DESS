package com.dess.institution;

import java.nio.file.Path;

/**
 * @brief Configuration settings of the institution module.
 *
 * <p>The XML file path is never hard-coded. It is resolved, in this order, from the system property
 * {@value #PROPERTY}, the environment variable {@value #ENV}, and finally from the default relative
 * path {@value #DEFAULT_RELATIVE_PATH} resolved against the working directory.</p>
 *
 * @param configFile path of the institution configuration XML file
 */
public record InstitutionSettings(Path configFile)
{
    /**
     * System property used to configure the XML file path.
     */
    public static final String PROPERTY = "dess.institution.config";

    /**
     * Environment variable used to configure the XML file path.
     */
    public static final String ENV = "DESS_INSTITUTION_CONFIG";

    /**
     * Default path, relative to the working directory.
     */
    public static final String DEFAULT_RELATIVE_PATH = "config/institution.xml";

    /**
     * Resolves the module settings from the system property, the environment variable or the
     * default path.
     *
     * @return the resolved settings
     */
    public static InstitutionSettings fromEnvironment()
    {
        String value = System.getProperty(PROPERTY);

        if (value == null || value.isBlank())
        {
            value = System.getenv(ENV);
        }

        if (value == null || value.isBlank())
        {
            value = DEFAULT_RELATIVE_PATH;
        }

        return new InstitutionSettings(Path.of(value));
    }
}
