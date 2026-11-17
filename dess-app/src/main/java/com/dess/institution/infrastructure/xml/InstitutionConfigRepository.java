package com.dess.institution.infrastructure.xml;

import com.dess.institution.application.spi.ConfigPersistence;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.exception.ConfigurationSaveException;
import com.dess.institution.exception.InstitutionException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Persistance de la configuration dans un fichier XML, source de vérité du module.
 * <p>
 * Écriture sûre : la nouvelle version est écrite dans un fichier temporaire du même dossier, relue
 * et validée par le XSD, puis l'ancien fichier est copié en {@code <fichier>.bak} avant d'être
 * remplacé par un déplacement atomique. Si une étape échoue, l'ancien fichier reste intact.
 * <p>
 * Les classes d'infrastructure ne doivent jamais être utilisées par les autres modules.
 */
public final class InstitutionConfigRepository implements ConfigPersistence {

    /** Point d'extension de test : exécuté juste avant le remplacement du fichier. */
    @FunctionalInterface
    interface BeforeReplaceHook {
        void run(Path temporaryFile) throws IOException;
    }

    private final Path file;
    private final XmlConfigLoader loader;
    private final XmlConfigWriter writer;
    private final BeforeReplaceHook beforeReplace;

    public InstitutionConfigRepository(Path file) {
        this(file, temp -> {
        });
    }

    InstitutionConfigRepository(Path file, BeforeReplaceHook beforeReplace) {
        this.file = file.toAbsolutePath().normalize();
        this.loader = new XmlConfigLoader();
        this.writer = new XmlConfigWriter();
        this.beforeReplace = beforeReplace;
    }

    public Path file() {
        return file;
    }

    public Path backupFile() {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    @Override
    public Optional<InstitutionConfig> load() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(loader.load(file));
    }

    @Override
    public synchronized void save(InstitutionConfig config) {
        byte[] bytes = writer.toBytes(config);
        Path temp = null;
        try {
            Path dir = file.getParent();
            Files.createDirectories(dir);
            temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
            Files.write(temp, bytes);

            // Relecture : on ne remplace jamais un fichier valide par un fichier qui ne se relirait pas.
            try {
                loader.load(temp);
            } catch (InstitutionException e) {
                throw new ConfigurationSaveException("Le XML produit est invalide, sauvegarde annulée : "
                        + e.getMessage(), e);
            }

            beforeReplace.run(temp);

            if (Files.exists(file)) {
                Files.copy(file, backupFile(), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            temp = null;
        } catch (IOException e) {
            throw new ConfigurationSaveException("Écriture impossible du fichier " + file + " : " + e.getMessage(), e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // nettoyage au mieux
                }
            }
        }
    }
}
