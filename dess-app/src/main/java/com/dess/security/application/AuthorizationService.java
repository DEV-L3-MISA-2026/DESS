package com.dess.security.application;

import com.dess.security.domain.Permission;
import com.dess.security.domain.UserAccount;

import java.util.Set;

/**
 * Answers what an account may do.
 *
 * <p>This is a skeleton. The implementation will read the account's roles and permissions and
 * apply the rules of the module.</p>
 */
public interface AuthorizationService
{
    /**
     * Checks whether an account has a permission.
     *
     * @param account account to check
     * @param permissionCode permission code to look for
     * @return true when one of the account's roles has the permission
     */
    boolean isPermitted(UserAccount account, String permissionCode);

    /**
     * Checks whether an account has a role.
     *
     * @param account account to check
     * @param roleCode role code to look for
     * @return true when the account has the role
     */
    boolean hasRole(UserAccount account, String roleCode);

    /**
     * Returns all permissions granted to an account.
     *
     * @param account account to check
     * @return the permissions from all of the account's roles
     */
    Set<Permission> permissionsOf(UserAccount account);
}
