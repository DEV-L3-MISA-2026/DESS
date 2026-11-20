package com.dess.security.application;

import com.dess.security.application.impl.AuthenticationServiceImpl;
import com.dess.security.application.spi.PasswordEncoder;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import com.dess.security.infrastructure.password.Pbkdf2PasswordEncoder;
import com.dess.security.infrastructure.persistence.InMemoryUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the authentication service.
 */
class AuthenticationServiceTest
{
    private UserAccountRepository repository;
    private PasswordEncoder encoder;
    private AuthenticationService authService;

    @BeforeEach
    void setUp()
    {
        encoder = new Pbkdf2PasswordEncoder();
        repository = new InMemoryUserAccountRepository(encoder, "admin123", "prof123");
        authService = new AuthenticationServiceImpl(repository, encoder);
    }

    @Test
    void authenticatesAdminSuccessfullyWithCorrectPassword()
    {
        Optional<UserAccount> account = authService.authenticate("admin", "admin123");

        assertTrue(account.isPresent());
        assertEquals("admin", account.get().username());
        assertTrue(account.get().roles().stream().anyMatch(r -> "ADMIN".equals(r.code())));
    }

    @Test
    void authenticatesProfessorSuccessfullyWithCorrectPassword()
    {
        Optional<UserAccount> account = authService.authenticate("professor", "prof123");

        assertTrue(account.isPresent());
        assertEquals("professor", account.get().username());
        assertTrue(account.get().roles().stream().anyMatch(r -> "PROFESSOR".equals(r.code())));
    }

    @Test
    void rejectsAuthenticationWithWrongPassword()
    {
        Optional<UserAccount> account = authService.authenticate("admin", "wrong_password");

        assertFalse(account.isPresent());
    }

    @Test
    void rejectsAuthenticationForUnknownUser()
    {
        Optional<UserAccount> account = authService.authenticate("unknown_user", "secret");

        assertFalse(account.isPresent());
    }

    @Test
    void rejectsAuthenticationForInactiveUser()
    {
        UserAccount inactiveUser = new UserAccount(
                "usr-inactive",
                "inactive",
                "Inactive Account",
                encoder.encode("secret123"),
                false,
                Set.of(new Role("ADMIN", "Admin", Set.of())));

        repository.save(inactiveUser);

        assertFalse(authService.authenticate("inactive", "secret123").isPresent());
    }

    @Test
    void rejectsNullInputs()
    {
        assertFalse(authService.authenticate(null, "admin123").isPresent());
        assertFalse(authService.authenticate("admin", null).isPresent());
    }

    @Test
    void doesNotSeedDefaultAccountsWithoutEnvironmentPasswords()
    {
        UserAccountRepository empty = new InMemoryUserAccountRepository(
                encoder, null, null);

        assertTrue(empty.findAll().isEmpty());
    }
}
