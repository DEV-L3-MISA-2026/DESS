package com.dess.security.infrastructure.persistence;

import com.dess.security.application.spi.PasswordEncoder;
import com.dess.security.domain.Role;
import com.dess.security.domain.RoleCatalog;
import com.dess.security.domain.UserAccount;
import com.dess.security.domain.UserAccountRepository;
import com.dess.security.infrastructure.password.Pbkdf2PasswordEncoder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory account repository used until accounts are persisted.
 *
 * <p>No default password ships in the code. The repository seeds the administrator and
 * professor accounts only when the environment supplies
 * {@code DESS_ADMIN_PASSWORD} and {@code DESS_PROFESSOR_PASSWORD}. When neither is set,
 * the application starts without accounts and the login page explains how to create
 * them.</p>
 */
@ApplicationScoped
public class InMemoryUserAccountRepository implements UserAccountRepository
{
    static final String ADMIN_PASSWORD_ENV = "DESS_ADMIN_PASSWORD";
    static final String PROFESSOR_PASSWORD_ENV = "DESS_PROFESSOR_PASSWORD";

    private static final Logger LOGGER =
            Logger.getLogger(InMemoryUserAccountRepository.class.getName());

    private final Map<String, UserAccount> usersByUsername = new ConcurrentHashMap<>();

    /**
     * Creates the repository and seeds accounts from the environment.
     */
    public InMemoryUserAccountRepository()
    {
        this(new Pbkdf2PasswordEncoder(),
                System.getenv(ADMIN_PASSWORD_ENV),
                System.getenv(PROFESSOR_PASSWORD_ENV));
    }

    /**
     * Creates the repository with explicit passwords. Tests use this constructor; the
     * environment is read only by the no-argument constructor.
     *
     * @param encoder password encoder
     * @param adminPassword administrator password, or {@code null} to skip the account
     * @param professorPassword professor password, or {@code null} to skip the account
     */
    public InMemoryUserAccountRepository(PasswordEncoder encoder, String adminPassword,
                                         String professorPassword)
    {
        if (adminPassword != null && !adminPassword.isBlank())
        {
            save(new UserAccount("usr-admin", "admin", "System Administrator",
                    encoder.encode(adminPassword), true, Set.of(RoleCatalog.admin())));
        }

        if (professorPassword != null && !professorPassword.isBlank())
        {
            save(new UserAccount("usr-prof", "professor", "Principal Professor",
                    encoder.encode(professorPassword), true, Set.of(RoleCatalog.professor())));
        }

        if (usersByUsername.isEmpty())
        {
            LOGGER.warning("No user accounts were seeded. Set " + ADMIN_PASSWORD_ENV
                    + " and/or " + PROFESSOR_PASSWORD_ENV
                    + " before starting the application to create the initial accounts.");
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username)
    {
        if (username == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByUsername.get(normalize(username)));
    }

    @Override
    public List<UserAccount> findAll()
    {
        return usersByUsername.values().stream()
                .sorted(Comparator.comparing(UserAccount::username))
                .toList();
    }

    @Override
    public void save(UserAccount account)
    {
        if (account == null || account.username() == null)
        {
            return;
        }
        usersByUsername.put(normalize(account.username()), account);
    }

    @Override
    public void delete(String id)
    {
        if (id == null)
        {
            return;
        }
        usersByUsername.values().removeIf(account -> id.equals(account.id()));
    }

    private static String normalize(String username)
    {
        return username.trim().toLowerCase();
    }
}
