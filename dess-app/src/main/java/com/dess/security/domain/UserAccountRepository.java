package com.dess.security.domain;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for user accounts.
 *
 * <p>The application layer depends on this interface. The current adapter keeps accounts
 * in memory; a database adapter can replace it without changing the services.</p>
 */
public interface UserAccountRepository
{
    /**
     * Finds an account by its login name.
     *
     * @param username login name
     * @return the account, or an empty result when it does not exist
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Returns every account.
     *
     * @return all accounts, including inactive ones
     */
    List<UserAccount> findAll();

    /**
     * Saves or updates an account.
     *
     * @param account account to save
     */
    void save(UserAccount account);

    /**
     * Deletes an account.
     *
     * @param id account identifier
     */
    void delete(String id);
}
