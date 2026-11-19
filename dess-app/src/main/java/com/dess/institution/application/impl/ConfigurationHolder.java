package com.dess.institution.application.impl;

import com.dess.institution.application.spi.ConfigPersistence;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.exception.NotInitializedException;
import com.dess.institution.exception.ValidationException;
import com.dess.institution.validation.InstitutionValidator;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @brief Holds the single in-memory configuration and keeps it synchronized with persistence.
 *
 * <p>Every write follows the same protocol under mutual exclusion: compute a new state from the
 * current state (which is never modified in place), validate the complete result, persist it, then
 * publish it in memory. If any step fails, neither the file nor the in-memory state changes.
 * Reads do not acquire the monitor because the published snapshot is immutable and volatile.</p>
 *
 * <p>Limitation: the monitor protects a single JVM. Multiple instances sharing the same file are
 * not supported.</p>
 */
public final class ConfigurationHolder
{
    /**
     * Result of a write operation: the new configuration and the value returned to the caller.
     *
     * @param config new configuration to validate and persist
     * @param result value returned to the caller
     * @param <R>    result type
     */
    public record Change<R>(InstitutionConfig config, R result)
    {
    }

    private final ConfigPersistence persistence;
    private volatile InstitutionConfig current;

    /**
     * Creates a holder backed by the supplied persistence.
     *
     * @param persistence persistence port
     * @throws NullPointerException if {@code persistence} is {@code null}
     */
    public ConfigurationHolder(ConfigPersistence persistence)
    {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
    }

    /**
     * Reloads and validates the configuration from persistence.
     *
     * <p>The in-memory state is replaced only after the loaded configuration has been validated. An
     * absent configuration resets the holder to the not-initialized state.</p>
     *
     * @throws com.dess.institution.exception.ConfigurationLoadException if the stored configuration cannot be read
     * @throws ValidationException if the loaded configuration is invalid
     */
    public synchronized void load()
    {
        Optional<InstitutionConfig> loaded = persistence.load();
        loaded.ifPresent(InstitutionValidator::validateOrThrow);
        current = loaded.orElse(null);
    }

    /**
     * Indicates whether a configuration is currently loaded.
     *
     * @return {@code true} when a configuration is available
     */
    public boolean isInitialized()
    {
        return current != null;
    }

    /**
     * Returns the current immutable configuration snapshot.
     *
     * @return the current configuration
     * @throws NotInitializedException if no configuration is loaded
     */
    public InstitutionConfig current()
    {
        InstitutionConfig snapshot = current;

        if (snapshot == null)
        {
            throw new NotInitializedException();
        }

        return snapshot;
    }

    /**
     * Installs the first configuration, as explicitly requested by the user.
     *
     * @param initial configuration to install
     * @throws NullPointerException if {@code initial} is {@code null}
     * @throws ValidationException if the institution is already initialized or the configuration is invalid
     */
    public synchronized void initialize(InstitutionConfig initial)
    {
        Objects.requireNonNull(initial, "initial");

        if (current != null)
        {
            throw new ValidationException("The institution is already initialized");
        }

        InstitutionValidator.validateOrThrow(initial);
        persistence.save(initial);
        current = initial;
    }

    /**
     * Applies a modification atomically and serially.
     *
     * @param operation function that computes the new configuration and the value to return
     * @param <R>       result type
     * @return the value produced by {@code operation}
     * @throws NullPointerException if {@code operation} is {@code null} or returns {@code null}
     * @throws NotInitializedException if no configuration is loaded
     * @throws ValidationException if the resulting configuration is invalid
     */
    public synchronized <R> R update(Function<InstitutionConfig, Change<R>> operation)
    {
        Objects.requireNonNull(operation, "operation");

        InstitutionConfig base = current();
        Change<R> change = Objects.requireNonNull(operation.apply(base), "change");

        InstitutionValidator.validateOrThrow(change.config());
        persistence.save(change.config());
        current = change.config();

        return change.result();
    }
}
