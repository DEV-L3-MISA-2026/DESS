package com.dess.security.application;

import com.dess.security.domain.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @brief Administers accounts and their role assignments.
 *
 * <p>Scaffold only: the interface declares the user-management use cases. An implementation will
 * validate uniqueness, hash passwords through the password adapter and persist changes through the
 * account repository.</p>
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
     * @return the account, or an empty optional when it does not exist
     */
    Optional<UserAccount> findById(String id);

    /**
     * Finds an account by login name.
     *
     * @param username login name
     * @return the account, or an empty optional when it does not exist
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Creates an account.
     *
     * @param username    unique login name
     * @param rawPassword plain-text initial password; implementations must not retain it
     * @param displayName name shown in the interface
     * @param roleCodes   codes of the roles to grant
     * @return the created account
     */
    UserAccount create(String username, String rawPassword, String displayName, Set<String> roleCodes);

    /**
     * Updates the display name of an account.
     *
     * @param id          account identifier
     * @param displayName new name shown in the interface
     * @return the updated account
     */
    UserAccount updateProfile(String id, String displayName);

    /**
     * Replaces the roles granted to an account.
     *
     * @param id        account identifier
     * @param roleCodes codes of the roles to grant
     * @return the updated account
     */
    UserAccount assignRoles(String id, Set<String> roleCodes);

    /**
     * Replaces the password of an account.
     *
     * @param id          account identifier
     * @param rawPassword new plain-text password; implementations must not retain it
     */
    void changePassword(String id, String rawPassword);

    /**
     * Activates or deactivates an account.
     *
     * @param id     account identifier
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
