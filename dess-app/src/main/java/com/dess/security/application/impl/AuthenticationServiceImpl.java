package com.dess.security.application.impl;

import com.dess.security.application.AuthenticationService;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import com.dess.security.infrastructure.password.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Implémentation du service d'authentification pour le MVP.
 *
 * Utilité : Vérifie les identifiants reçus.
 * 1. Recherche le compte par username.
 * 2. Vérifie qu'il est actif.
 * 3. Vérifie la concordance du mot de passe avec le hash stocké.
 */
@ApplicationScoped
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;

    // Constructeur sans argument requis par CDI
    public AuthenticationServiceImpl() {
        this(null, new PasswordHasher());
    }

    @Inject
    public AuthenticationServiceImpl(UserAccountRepository repository) {
        this(repository, new PasswordHasher());
    }

    public AuthenticationServiceImpl(UserAccountRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<UserAccount> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null || repository == null) {
            return Optional.empty();
        }

        return repository.findByUsername(username)
                .filter(UserAccount::active)
                .filter(account -> passwordHasher.matches(rawPassword, account.passwordHash()));
    }
}
