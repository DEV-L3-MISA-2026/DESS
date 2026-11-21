package com.dess.security.application;

import com.dess.security.application.impl.AuthorizationServiceImpl;
import com.dess.security.domain.RoleCatalog;
import com.dess.security.domain.UserAccount;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for permission checks.
 */
class AuthorizationServiceTest
{
    private final AuthorizationService service = new AuthorizationServiceImpl();
    private final UserAccount admin = new UserAccount("u1", "admin", "Admin", null, true,
            Set.of(RoleCatalog.admin()));
    private final UserAccount professor = new UserAccount("u2", "prof", "Professor", null, true,
            Set.of(RoleCatalog.professor()));

    @Test
    void administratorHasEveryPermission()
    {
        assertTrue(service.isPermitted(admin, RoleCatalog.PEOPLE_WRITE));
        assertTrue(service.isPermitted(admin, RoleCatalog.INSTITUTION_WRITE));
        assertTrue(service.isPermitted(admin, RoleCatalog.SECURITY_MANAGE));
        assertTrue(service.hasRole(admin, RoleCatalog.ADMIN));
    }

    @Test
    void professorHasReadPermissionsOnly()
    {
        assertTrue(service.isPermitted(professor, RoleCatalog.INSTITUTION_READ));
        assertTrue(service.isPermitted(professor, RoleCatalog.PEOPLE_READ));
        assertTrue(service.isPermitted(professor, RoleCatalog.ACADEMIC_READ));
        assertFalse(service.isPermitted(professor, RoleCatalog.INSTITUTION_WRITE));
        assertFalse(service.isPermitted(professor, RoleCatalog.PEOPLE_WRITE));
        assertFalse(service.isPermitted(professor, RoleCatalog.SECURITY_MANAGE));
    }

    @Test
    void unknownRoleAndNullAccountAreRefused()
    {
        assertFalse(service.hasRole(professor, "UNKNOWN"));
        assertFalse(service.isPermitted(null, RoleCatalog.PEOPLE_READ));
        assertTrue(service.permissionsOf(null).isEmpty());
    }
}
