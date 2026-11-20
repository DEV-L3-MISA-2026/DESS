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
        assertTrue(filter.construireJson401().contains("UNAUTHORIZED"));
        assertTrue(filter.construireJson403().contains("FORBIDDEN"));
    }
}
