package com.dess.platform.web.view;

import java.util.Objects;

/** One account row on the security page. */
public final class AccountRow
{
    private final String username;
    private final String displayName;
    private final String roles;
    private final Badge status;

    /**
     * @param username sign-in name
     * @param displayName visible name
     * @param roles comma-separated role codes
     * @param active whether the account is active
     */
    public AccountRow(String username, String displayName, String roles, boolean active)
    {
        this.username = Objects.requireNonNull(username, "username is required");
        this.displayName = Objects.requireNonNull(displayName, "displayName is required");
        this.roles = Objects.requireNonNull(roles, "roles is required");
        this.status = Badge.active(active);
    }

    public String getUsername()
    {
        return username;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getRoles()
    {
        return roles;
    }

    public Badge getStatus()
    {
        return status;
    }
}
