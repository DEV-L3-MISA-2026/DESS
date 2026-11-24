package com.dess.security.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure path rules of {@link SecurityFilter}.
 */
class SecurityFilterTest
{
    private SecurityFilter filter;

    @BeforeEach
    void setUp()
    {
        filter = new SecurityFilter();
    }

    @Test
    void healthAndLoginApiArePublic()
    {
        assertTrue(filter.estRoutePublique("/api/health"));
        assertTrue(filter.estRoutePublique("/api/auth/login"));
    }

    @Test
    void loginPageAndAssetsArePublic()
    {
        assertTrue(filter.estRoutePublique("/login"));
        assertTrue(filter.estRoutePublique("/assets/css/tokens.css"));
        assertTrue(filter.estRoutePublique("/assets/js/shell.js"));
    }

    @Test
    void applicationPagesAndOtherApisAreProtected()
    {
        assertFalse(filter.estRoutePublique("/"));
        assertFalse(filter.estRoutePublique("/app"));
        assertFalse(filter.estRoutePublique("/app/people"));
        assertFalse(filter.estRoutePublique("/api/users"));
        assertFalse(filter.estRoutePublique("/login/extra"));
    }

    @Test
    void nullAndEmptyPathsAreProtected()
    {
        assertFalse(filter.estRoutePublique(""));
        assertFalse(filter.estRoutePublique(null));
    }

    @Test
    void jsonErrorsContainCodeAndMessage()
    {
        assertTrue(SecurityFilter.construireJson401().contains("UNAUTHORIZED"));
        assertTrue(SecurityFilter.construireJson403().contains("FORBIDDEN"));
    }

    @Test
    void institutionWritesAreDetectedOnRootEditorAndAliases()
    {
        assertTrue(filter.estEcritureInstitution("/", "POST"));
        assertTrue(filter.estEcritureInstitution("", "POST"));
        assertTrue(filter.estEcritureInstitution("/index.jsp", "POST"));
        assertTrue(filter.estEcritureInstitution("/", "post"));
        assertTrue(filter.estEcritureInstitution("/index.jsp;foo", "POST"));
        assertTrue(filter.estEcritureInstitution("/%69ndex.jsp", "POST"));
        assertTrue(filter.estEcritureInstitution("//index.jsp", "POST"));
        assertTrue(filter.estEcritureInstitution("/;x", "POST"));
    }

    @Test
    void institutionReadsAndKnownWriteAreasAreNotInstitutionWrites()
    {
        assertFalse(filter.estEcritureInstitution("/", "GET"));
        assertFalse(filter.estEcritureInstitution("/index.jsp", "HEAD"));
        assertFalse(filter.estEcritureInstitution("/", "OPTIONS"));
        assertFalse(filter.estEcritureInstitution("/app/people", "POST"));
        assertFalse(filter.estEcritureInstitution("/api/users", "POST"));
        assertFalse(filter.estEcritureInstitution("/login", "POST"));
        assertFalse(filter.estEcritureInstitution("/logout", "POST"));
        assertFalse(filter.estEcritureInstitution("/assets/css/tokens.css", "POST"));
        assertFalse(filter.estEcritureInstitution("/favicon.ico", "POST"));
        assertFalse(filter.estEcritureInstitution(null, "POST"));
        assertFalse(filter.estEcritureInstitution("/", null));
    }
}
