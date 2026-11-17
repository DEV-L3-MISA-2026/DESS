package com.dess.institution.application.impl;

import com.dess.institution.application.spi.ConfigPersistence;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.exception.NotInitializedException;
import com.dess.institution.exception.ValidationException;
import com.dess.institution.validation.InstitutionValidator;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Détient l'unique état en mémoire de la configuration et garantit sa synchronisation avec le fichier.
 * <p>
 * Toute écriture suit le même protocole, sous verrou : calcul d'un nouvel état à partir de l'état
 * courant (jamais modifié en place) → validation complète → persistance → publication en mémoire.
 * Si une étape échoue, ni le fichier ni la mémoire ne changent. Les lectures ne prennent pas le
 * verrou : elles voient toujours un instantané immuable et cohérent.
 * <p>
 * Limite : le verrou protège une seule JVM. Plusieurs instances partageant le même fichier ne sont
 * pas supportées.
 */
public final class ConfigurationHolder {

    /** Résultat d'une opération d'écriture : nouvel état et valeur à retourner à l'appelant. */
    public record Change<R>(InstitutionConfig config, R result) {
    }

    private final ConfigPersistence persistence;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile InstitutionConfig current;

    public ConfigurationHolder(ConfigPersistence persistence) {
        this.persistence = persistence;
    }

    /** (Re)charge depuis la persistance et valide. Lève une exception sans toucher à l'état si invalide. */
    public void load() {
        lock.lock();
        try {
            Optional<InstitutionConfig> loaded = persistence.load();
            loaded.ifPresent(InstitutionValidator::validateOrThrow);
            current = loaded.orElse(null);
        } finally {
            lock.unlock();
        }
    }

    public boolean isInitialized() {
        return current != null;
    }

    /** @throws NotInitializedException si aucune configuration n'est chargée */
    public InstitutionConfig current() {
        InstitutionConfig snapshot = current;
        if (snapshot == null) {
            throw new NotInitializedException();
        }
        return snapshot;
    }

    /** Installe la toute première configuration (explicitement demandée par l'utilisateur). */
    public void initialize(InstitutionConfig initial) {
        lock.lock();
        try {
            if (current != null) {
                throw new ValidationException("L'établissement est déjà initialisé");
            }
            InstitutionValidator.validateOrThrow(initial);
            persistence.save(initial);
            current = initial;
        } finally {
            lock.unlock();
        }
    }

    /** Applique une modification de façon atomique et sérialisée. */
    public <R> R update(Function<InstitutionConfig, Change<R>> operation) {
        lock.lock();
        try {
            InstitutionConfig base = current();
            Change<R> change = operation.apply(base);
            InstitutionValidator.validateOrThrow(change.config());
            persistence.save(change.config());
            current = change.config();
            return change.result();
        } finally {
            lock.unlock();
        }
    }
}
