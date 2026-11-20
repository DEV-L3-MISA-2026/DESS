package com.dess.security.application;

import com.dess.security.domain.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates and manages user accounts.
 *
 * <p>This is a skeleton. The implementation will check unique usernames, hash passwords and save
 * changes through the account repository.</p>
 */
public interface UserManagementService
{
    /**
     * Returns every account.
     *
     * @return all accounts, including inactive ones
     */
    List<UserAccount> findAll();

    /**
     * Finds an account by identifier.
     *
     * @param id account identifier
     * @return the account, or an empty result when it does not exist
     */
    Optional<UserAccount> findById(String id);

    /**
     * Finds an account by login name.
     *
     * @param username login name
     * @return the account, or an empty result when it does not exist
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Creates an account.
     *
     * @param username unique login name
     * @param rawPassword plain initial password; it must not be stored
     * @param displayName name shown in the interface
     * @param roleCodes codes of the roles to grant
     * @return the new account
     */
    UserAccount create(String username, String rawPassword, String displayName, Set<String> roleCodes);

    /**
     * Changes the display name of an account.
     *
     * @param id account identifier
     * @param displayName new name shown in the interface
     * @return the updated account
     */
    UserAccount updateProfile(String id, String displayName);

    /**
     * Replaces the roles granted to an account.
     *
     * @param id account identifier
     * @param roleCodes codes of the roles to grant
     * @return the updated account
     */
    UserAccount assignRoles(String id, Set<String> roleCodes);

    /**
     * Replaces the password of an account.
     *
     * @param id account identifier
     * @param rawPassword new plain password; it must not be stored
     */
    void changePassword(String id, String rawPassword);

    /**
     * Activates or deactivates an account.
     *
     * @param id account identifier
     * @param active new activation state
     */
    void setActive(String id, boolean active);

    /**
     * Deletes an account.
     *
     * @param id account identifier
     */
    void delete(String id);
}
