package com.dess.security.application;

import com.dess.security.domain.Permission;
import com.dess.security.domain.UserAccount;

import java.util.Set;

/**
 * @brief Answers authorization questions for an authenticated account.
 *
 * <p>Scaffold only: the interface declares the authorization use cases. An implementation will
 * resolve the roles and permissions carried by the account and apply the module's authorization
 * rules.</p>
 */
public interface AuthorizationService
{
    /**
     * Tests whether an account holds a permission.
     *
     * @param account        authenticated account
     * @param permissionCode permission code to check
     * @return {@code true} when the account has the permission through one of its roles
     */
    boolean isPermitted(UserAccount account, String permissionCode);

    /**
     * Tests whether an account holds a role.
     *
     * @param account  authenticated account
     * @param roleCode role code to check
     * @return {@code true} when the account has the role
     */
    boolean hasRole(UserAccount account, String roleCode);

    /**
     * Returns the effective permissions of an account.
     *
     * @param account authenticated account
     * @return the permissions granted by all of the account's roles
     */
    Set<Permission> permissionsOf(UserAccount account);
}
