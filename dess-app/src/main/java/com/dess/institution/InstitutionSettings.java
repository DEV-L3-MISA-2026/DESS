package com.dess.institution;

import java.nio.file.Path;

/**
 * Paramètres du module. Le chemin du fichier XML n'est jamais codé en dur : il est lu, dans cet ordre,
 * depuis la propriété système {@value #PROPERTY}, la variable d'environnement {@value #ENV},
 * puis par défaut {@code config/institution.xml} relatif au répertoire de travail.
 *
 * @param configFile chemin du fichier XML de configuration de l'établissement
 */
public record InstitutionSettings(Path configFile) {

    public static final String PROPERTY = "dess.institution.config";
    public static final String ENV = "DESS_INSTITUTION_CONFIG";
    public static final String DEFAULT_RELATIVE_PATH = "config/institution.xml";

    public static InstitutionSettings fromEnvironment() {
        String value = System.getProperty(PROPERTY);
        if (value == null || value.isBlank()) {
            value = System.getenv(ENV);
        }
        if (value == null || value.isBlank()) {
            value = DEFAULT_RELATIVE_PATH;
        }
        return new InstitutionSettings(Path.of(value));
    }
}
