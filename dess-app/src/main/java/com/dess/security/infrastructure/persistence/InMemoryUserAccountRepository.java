package com.dess.security.infrastructure.persistence;

import com.dess.security.domain.Permission;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import com.dess.security.infrastructure.password.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implémentation simple en mémoire du dépôt des comptes.
 *
 * Utilité : Fournit un stockage immédiatement fonctionnel pour le MVP
 * pré-rempli avec les comptes initiaux 'admin' et 'professeur'.
 */
@ApplicationScoped
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> usersByUsername = new ConcurrentHashMap<>();

    public InMemoryUserAccountRepository() {
        PasswordHasher hasher = new PasswordHasher();

        // Rôles minimaux du MVP : ADMIN et PROFESSOR
        Role adminRole = new Role("ADMIN", "Administrateur", Set.of(new Permission("ALL", "Tous les droits")));
        Role professorRole = new Role("PROFESSOR", "Professeur", Set.of(new Permission("TEACH", "Enseignement")));

        // Compte admin par défaut (mot de passe : admin123)
        UserAccount admin = new UserAccount(
                "usr-admin",
                "admin",
                "Administrateur Système",
                hasher.hash("admin123"),
                true,
                Set.of(adminRole)
        );

        // Compte professeur par défaut (mot de passe : prof123)
        UserAccount professor = new UserAccount(
                "usr-prof",
                "professeur",
                "Professeur Principal",
                hasher.hash("prof123"),
                true,
                Set.of(professorRole)
        );

        save(admin);
        save(professor);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByUsername.get(username.trim().toLowerCase()));
    }

    @Override
    public void save(UserAccount account) {
        if (account != null && account.username() != null) {
            usersByUsername.put(account.username().trim().toLowerCase(), account);
        }
    }
}
