package com.dess.security.application;

import com.dess.security.application.impl.AuthenticationServiceImpl;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import com.dess.security.infrastructure.password.PasswordHasher;
import com.dess.security.infrastructure.persistence.InMemoryUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires pour le service d'authentification.
 */
class AuthenticationServiceTest {

    private UserAccountRepository repository;
    private PasswordHasher hasher;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
        repository = new InMemoryUserAccountRepository();
        authService = new AuthenticationServiceImpl(repository, hasher);
    }

    @Test
    void authenticatesAdminSuccessfullyWithCorrectPassword() {
        Optional<UserAccount> account = authService.authenticate("admin", "admin123");

        assertTrue(account.isPresent());
        assertEquals("admin", account.get().username());
        assertTrue(account.get().roles().stream().anyMatch(r -> "ADMIN".equals(r.code())));
    }

    @Test
    void authenticatesProfessorSuccessfullyWithCorrectPassword() {
        Optional<UserAccount> account = authService.authenticate("professeur", "prof123");

        assertTrue(account.isPresent());
        assertEquals("professeur", account.get().username());
        assertTrue(account.get().roles().stream().anyMatch(r -> "PROFESSOR".equals(r.code())));
    }

    @Test
    void rejectsAuthenticationWithWrongPassword() {
        Optional<UserAccount> account = authService.authenticate("admin", "mauvais_mot_de_passe");

        assertFalse(account.isPresent());
    }

    @Test
    void rejectsAuthenticationForUnknownUser() {
        Optional<UserAccount> account = authService.authenticate("utilisateur_inconnu", "secret");

        assertFalse(account.isPresent());
    }

    @Test
    void rejectsAuthenticationForInactiveUser() {
        UserAccount inactiveUser = new UserAccount(
                "usr-inactive",
                "inactif",
                "Compte Inactif",
                hasher.hash("secret123"),
                false, // Inactif
                Set.of(new Role("ADMIN", "Admin", Set.of()))
        );
        repository.save(inactiveUser);

        Optional<UserAccount> account = authService.authenticate("inactif", "secret123");

        assertFalse(account.isPresent());
    }

    @Test
    void rejectsNullInputs() {
        assertFalse(authService.authenticate(null, "admin123").isPresent());
        assertFalse(authService.authenticate("admin", null).isPresent());
    }
}
