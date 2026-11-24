package com.dess.security.application.impl;

import com.dess.security.application.AuthenticationService;
import com.dess.security.application.spi.PasswordEncoder;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * Checks a username and password against the account repository.
 *
 * <p>A failed login never reveals whether the username exists: unknown, inactive and
 * wrong-password accounts all return an empty result.</p>
 */
@ApplicationScoped
public class AuthenticationServiceImpl implements AuthenticationService
{
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public AuthenticationServiceImpl(UserAccountRepository repository,
                                     PasswordEncoder passwordEncoder)
    {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<UserAccount> authenticate(String username, String rawPassword)
    {
        if (username == null || rawPassword == null || repository == null)
        {
            return Optional.empty();
        }

        return repository.findByUsername(username)
                .filter(UserAccount::active)
                .filter(account -> passwordEncoder.matches(rawPassword, account.passwordHash()));
    }
}
