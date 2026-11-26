package com.dess.security.application.impl;

import com.dess.security.application.UserManagementService;
import com.dess.security.application.spi.PasswordEncoder;
import com.dess.security.domain.Role;
import com.dess.security.domain.RoleCatalog;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates and manages accounts.
 *
 * <p>The service keeps usernames unique, hashes every password through the encoder and
 * never returns a plain password.</p>
 */
@ApplicationScoped
public class UserManagementServiceImpl implements UserManagementService
{
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public UserManagementServiceImpl(UserAccountRepository repository,
                                     PasswordEncoder passwordEncoder)
    {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserAccount> findAll()
    {
        return repository.findAll();
    }

    @Override
    public Optional<UserAccount> findById(String id)
    {
        if (id == null)
        {
            return Optional.empty();
        }

        return repository.findAll().stream()
                .filter(account -> id.equals(account.id()))
                .findFirst();
    }

    @Override
    public Optional<UserAccount> findByUsername(String username)
    {
        return repository.findByUsername(username);
    }

    @Override
    public UserAccount create(String username, String rawPassword, String displayName,
                              Set<String> roleCodes)
    {
        String normalizedUsername = requireText(username, "A username is required");
        String normalizedName = requireText(displayName, "A display name is required");
        requireText(rawPassword, "A password is required");

        if (repository.findByUsername(normalizedUsername).isPresent())
        {
            throw new IllegalArgumentException("This username is already in use");
        }

        Set<Role> roles = resolveRoles(roleCodes);
        UserAccount account = new UserAccount(
                "usr-" + UUID.randomUUID(),
                normalizedUsername,
                normalizedName,
                passwordEncoder.encode(rawPassword),
                true,
                roles);

        repository.save(account);
        return account;
    }

    @Override
    public UserAccount updateProfile(String id, String displayName)
    {
        UserAccount account = requireAccount(id);
        UserAccount updated = new UserAccount(account.id(), account.username(),
                requireText(displayName, "A display name is required"),
                account.passwordHash(), account.active(), account.roles());
        repository.save(updated);
        return updated;
    }

    @Override
    public UserAccount assignRoles(String id, Set<String> roleCodes)
    {
        UserAccount account = requireAccount(id);
        UserAccount updated = new UserAccount(account.id(), account.username(),
                account.displayName(), account.passwordHash(), account.active(),
                resolveRoles(roleCodes));
        repository.save(updated);
        return updated;
    }

    @Override
    public void changePassword(String id, String rawPassword)
    {
        UserAccount account = requireAccount(id);
        requireText(rawPassword, "A password is required");

        repository.save(new UserAccount(account.id(), account.username(),
                account.displayName(), passwordEncoder.encode(rawPassword),
                account.active(), account.roles()));
    }

    @Override
    public void setActive(String id, boolean active)
    {
        UserAccount account = requireAccount(id);
        repository.save(new UserAccount(account.id(), account.username(),
                account.displayName(), account.passwordHash(), active, account.roles()));
    }

    @Override
    public void delete(String id)
    {
        requireAccount(id);
        repository.delete(id);
    }

    private UserAccount requireAccount(String id)
    {
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private static Set<Role> resolveRoles(Set<String> roleCodes)
    {
        if (roleCodes == null || roleCodes.isEmpty())
        {
            throw new IllegalArgumentException("Select at least one role");
        }

        return roleCodes.stream()
                .map(code -> RoleCatalog.byCode(code)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown role: " + code)))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String requireText(String value, String message)
    {
        if (value == null || value.trim().isBlank())
        {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
