package com.dess.security.web;

import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The part of an account that is safe to keep in the HTTP session.
 *
 * <p>It deliberately carries no password hash. The JSP layer reads the JavaBean
 * properties (for example {@code ${sessionUser.displayName}}), and the servlets use
 * {@link #isPermitted(String)} for access checks.</p>
 */
public final class SessionUser
{
    /** Session attribute that holds the signed-in user. */
    public static final String SESSION_ATTRIBUTE = "sessionUser";

    private final String id;
    private final String username;
    private final String displayName;
    private final Set<Role> roles;

    private SessionUser(String id, String username, String displayName, Set<Role> roles)
    {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roles = Set.copyOf(roles);
    }

    /**
     * Builds the session view of an authenticated account.
     *
     * @param account authenticated account
     * @return the session-safe user
     */
    public static SessionUser from(UserAccount account)
    {
        return new SessionUser(account.id(), account.username(), account.displayName(),
                account.roles());
    }

    public String getId()
    {
        return id;
    }

    public String getUsername()
    {
        return username;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Set<String> getRoleCodes()
    {
        return roles.stream().map(Role::code).collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin()
    {
        return hasRole("ADMIN");
    }

    public boolean hasRole(String roleCode)
    {
        return roleCode != null && roles.stream().anyMatch(role -> roleCode.equals(role.code()));
    }

    public boolean isPermitted(String permissionCode)
    {
        if (permissionCode == null)
        {
            return false;
        }

        return roles.stream()
                .flatMap(role -> role.permissions().stream())
                .anyMatch(permission -> "ALL".equals(permission.code())
                        || permissionCode.equals(permission.code()));
    }

    /**
     * @return initials for the account chip
     */
    public String getInitials()
    {
        String[] parts = displayName.trim().split("\\s+");

        if (parts.length == 1)
        {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    /**
     * Rebuilds an account view for authorization checks. The password hash is absent on
     * purpose.
     *
     * @return the account without a password hash
     */
    public UserAccount toAccount()
    {
        return new UserAccount(id, username, displayName, null, true, roles);
    }
}
