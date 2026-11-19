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
import java.util.Objects;
import java.util.Optional;

/**
 * @brief XML file persistence for the institution configuration.
 *
 * <p>The XML file is the source of truth of the module. Writes are safe: the new version is written
 * to a temporary file in the same directory, read back and validated against the XSD, then the
 * previous file is copied to {@code <file>.bak} before being replaced by an atomic move. If any step
 * fails, the previous file remains intact.</p>
 *
 * <p>Infrastructure classes must never be used by other modules directly; they are wired by
 * {@link com.dess.institution.InstitutionModule}.</p>
 */
public final class InstitutionConfigRepository implements ConfigPersistence
{
    /**
     * Test extension point executed immediately before the file replacement.
     */
    @FunctionalInterface
    interface BeforeReplaceHook
    {
        /**
         * Runs before the temporary file replaces the current configuration file.
         *
         * @param temporaryFile validated temporary file
         * @throws IOException if the hook fails
         */
        void run(Path temporaryFile) throws IOException;
    }

    private final Path file;
    private final XmlConfigLoader loader;
    private final XmlConfigWriter writer;
    private final BeforeReplaceHook beforeReplace;

    /**
     * Creates a repository for the supplied XML file.
     *
     * @param file configuration file path
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public InstitutionConfigRepository(Path file)
    {
        this(file, temporaryFile ->
        {
        });
    }

    /**
     * Creates a repository with a test hook.
     *
     * @param file          configuration file path
     * @param beforeReplace hook executed before the file replacement
     * @throws NullPointerException if {@code file} or {@code beforeReplace} is {@code null}
     */
    InstitutionConfigRepository(Path file, BeforeReplaceHook beforeReplace)
    {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        this.loader = new XmlConfigLoader();
        this.writer = new XmlConfigWriter();
        this.beforeReplace = Objects.requireNonNull(beforeReplace, "beforeReplace");
    }

    @Override
    public Optional<InstitutionConfig> load()
    {
        if (!Files.exists(file))
        {
            return Optional.empty();
        }

        return Optional.of(loader.load(file));
    }

    @Override
    public synchronized void save(InstitutionConfig config)
    {
        byte[] bytes = writer.toBytes(config);
        Path temporaryFile = null;

        try
        {
            Path directory = file.getParent();
            Files.createDirectories(directory);

            temporaryFile = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
            Files.write(temporaryFile, bytes);

            // Read the produced XML back: a valid file is never replaced by one that cannot be reloaded.
            try
            {
                loader.load(temporaryFile);
            }
            catch (InstitutionException e)
            {
                throw new ConfigurationSaveException("The produced XML is invalid; save cancelled: "
                                                     + e.getMessage(), e);
            }

            beforeReplace.run(temporaryFile);

            if (Files.exists(file))
            {
                Files.copy(file, backupFile(), StandardCopyOption.REPLACE_EXISTING);
            }

            try
            {
                Files.move(
                    temporaryFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }

            temporaryFile = null;
        }
        catch (IOException e)
        {
            throw new ConfigurationSaveException(
                "Unable to write file " + file + ": " + e.getMessage(),
                e
            );
        }
        finally
        {
            if (temporaryFile != null)
            {
                try
                {
                    Files.deleteIfExists(temporaryFile);
                }
                catch (IOException ignored)
                {
                    // Best-effort cleanup; the original exception, if any, remains the relevant one.
                }
            }
        }
    }

    /**
     * Returns the backup file path.
     *
     * @return sibling path suffixed with {@code .bak}
     */
    private Path backupFile()
    {
        return file.resolveSibling(file.getFileName() + ".bak");
    }
}
