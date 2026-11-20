package com.dess.security.domain;

import java.util.Optional;
import java.util.Set;

/**
 * The roles used by DESS.
 *
 * <p>The catalog keeps role and permission codes in one place so the web layer, the
 * repository and the tests do not drift apart.</p>
 */
public final class RoleCatalog
{
    public static final String ADMIN = "ADMIN";
    public static final String PROFESSOR = "PROFESSOR";

    public static final String PERMISSION_ALL = "ALL";
    public static final String INSTITUTION_READ = "institution:read";
    public static final String INSTITUTION_WRITE = "institution:write";
    public static final String PEOPLE_READ = "people:read";
    public static final String PEOPLE_WRITE = "people:write";
    public static final String ACADEMIC_READ = "academic:read";
    public static final String ACADEMIC_WRITE = "academic:write";
    public static final String SCHEDULING_READ = "scheduling:read";
    public static final String SCHEDULING_WRITE = "scheduling:write";
    public static final String SECURITY_MANAGE = "security:manage";

    private RoleCatalog()
    {
    }

    /**
     * @return the administrator role, which may do everything
     */
    public static Role admin()
    {
        return new Role(ADMIN, "Administrator",
                Set.of(new Permission(PERMISSION_ALL, "All actions")));
    }

    /**
     * @return the professor role, which may read the institution and academic data
     */
    public static Role professor()
    {
        return new Role(PROFESSOR, "Professor", Set.of(
                new Permission(INSTITUTION_READ, "View the institution structure"),
                new Permission(PEOPLE_READ, "View people"),
                new Permission(ACADEMIC_READ, "View academic data"),
                new Permission(SCHEDULING_READ, "View the timetable")));
    }

    /**
     * Resolves a role code.
     *
     * @param code role code, case-insensitive
     * @return the matching role, or an empty result for an unknown code
     */
    public static Optional<Role> byCode(String code)
    {
        if (code == null)
        {
            return Optional.empty();
        }

        return switch (code.trim().toUpperCase())
        {
            case ADMIN -> Optional.of(admin());
            case PROFESSOR -> Optional.of(professor());
            default -> Optional.empty();
        };
    }
}
