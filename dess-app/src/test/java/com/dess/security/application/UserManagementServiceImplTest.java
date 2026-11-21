package com.dess.security.application;

import com.dess.security.application.impl.UserManagementServiceImpl;
import com.dess.security.domain.RoleCatalog;
import com.dess.security.domain.UserAccount;
import com.dess.security.infrastructure.password.Pbkdf2PasswordEncoder;
import com.dess.security.infrastructure.persistence.InMemoryUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for account management.
 */
class UserManagementServiceImplTest
{
    private InMemoryUserAccountRepository repository;
    private UserManagementService service;

    @BeforeEach
    void setUp()
    {
        Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder();
        repository = new InMemoryUserAccountRepository(encoder, null, null);
        service = new UserManagementServiceImpl(repository, encoder);
    }

    @Test
    void createsAnAccountWithAHashedPassword()
    {
        UserAccount account = service.create("secretary", "initial-pass", "Department Secretary",
                Set.of(RoleCatalog.PROFESSOR));

        assertEquals("secretary", account.username());
        assertTrue(account.active());
        assertNotEquals("initial-pass", account.passwordHash());
        assertTrue(account.passwordHash().startsWith("pbkdf2-sha256$"));
        assertTrue(account.roles().stream()
                .anyMatch(role -> RoleCatalog.PROFESSOR.equals(role.code())));
    }

    @Test
    void refusessDuplicateUsernames()
    {
        service.create("secretary", "initial-pass", "Department Secretary",
                Set.of(RoleCatalog.PROFESSOR));

        assertThrows(IllegalArgumentException.class, () -> service.create(
                "SECRETARY", "another-pass", "Another Person", Set.of(RoleCatalog.PROFESSOR)));
    }

    @Test
    void refusesUnknownRolesAndBlankInput()
    {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                "user", "pass", "User", Set.of("UNKNOWN")));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                " ", "pass", "User", Set.of(RoleCatalog.PROFESSOR)));
    }

    @Test
    void changesPasswordAndDeactivatesTheAccount()
    {
        UserAccount account = service.create("user", "first-pass", "User",
                Set.of(RoleCatalog.PROFESSOR));

        service.changePassword(account.id(), "second-pass");
        UserAccount updated = service.findById(account.id()).orElseThrow();

        assertNotEquals(account.passwordHash(), updated.passwordHash());

        service.setActive(account.id(), false);
        assertFalse(service.findById(account.id()).orElseThrow().active());
    }

    @Test
    void listsAccountsSortedByUsername()
    {
        service.create("zoe", "pass", "Zoe", Set.of(RoleCatalog.PROFESSOR));
        service.create("alice", "pass", "Alice", Set.of(RoleCatalog.PROFESSOR));

        assertEquals("alice", service.findAll().get(0).username());
        assertEquals("zoe", service.findAll().get(1).username());
    }
}
