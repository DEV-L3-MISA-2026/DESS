package com.dess.institution;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Utilitaires de test : accès aux ressources XML et copies jetables (les vrais fichiers ne sont jamais modifiés). */
public final class TestSupport {

    private TestSupport() {
    }

    public static Path resource(String relativePath) {
        try {
            var url = TestSupport.class.getResource("/" + relativePath);
            if (url == null) {
                throw new IllegalArgumentException("Ressource de test introuvable : " + relativePath);
            }
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Copie une ressource (ex. {@code valid/university.xml}) dans {@code dir/institution.xml}. */
    public static Path copyTo(Path dir, String resourcePath) {
        try (InputStream in = Files.newInputStream(resource(resourcePath))) {
            Path target = dir.resolve("institution.xml");
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ouvre le module sur une copie temporaire de la ressource donnée. */
    public static InstitutionModule openCopy(Path dir, String resourcePath) {
        return InstitutionModule.open(copyTo(dir, resourcePath));
    }

    public static InstitutionModule openUniversity(Path dir) {
        return openCopy(dir, "valid/university.xml");
    }
}
