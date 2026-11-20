package com.dess.security.application.impl;

import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.Permission;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answers what an account may do.
 *
 * <p>The permission {@code ALL} grants every action. Otherwise the account needs the
 * exact permission code through one of its roles.</p>
 */
@ApplicationScoped
public class AuthorizationServiceImpl implements AuthorizationService
{
    @Override
    public boolean isPermitted(UserAccount account, String permissionCode)
    {
        if (account == null || permissionCode == null)
        {
            return false;
        }

        return account.roles().stream()
                .flatMap(role -> role.permissions().stream())
                .anyMatch(permission -> "ALL".equals(permission.code())
                        || permissionCode.equals(permission.code()));
    }

    @Override
    public boolean hasRole(UserAccount account, String roleCode)
    {
        if (account == null || roleCode == null)
        {
            return false;
        }

        return account.roles().stream()
                .map(Role::code)
                .anyMatch(roleCode::equals);
    }

    @Override
    public Set<Permission> permissionsOf(UserAccount account)
    {
        if (account == null)
        {
            return Set.of();
        }

        return account.roles().stream()
                .flatMap(role -> role.permissions().stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
