package com.dess.security.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour SecurityFilter.
 *
 * On teste uniquement la logique pure (sans servlet container) :
 *   - estRoutePublique()
 *   - construireJson401()
 */
class SecurityFilterTest {

    private SecurityFilter filtre;

    @BeforeEach
    void setUp() {
        filtre = new SecurityFilter();
    }

    // --- Tests pour estRoutePublique() ---

    @Test
    void health_est_public() {
        assertTrue(filtre.estRoutePublique("/dess-app/api/health"));
    }

    @Test
    void login_est_public() {
        assertTrue(filtre.estRoutePublique("/dess-app/api/auth/login"));
    }

    @Test
    void autre_route_nest_pas_public() {
        assertFalse(filtre.estRoutePublique("/dess-app/api/users"));
    }

    @Test
    void route_vide_nest_pas_public() {
        assertFalse(filtre.estRoutePublique(""));
    }

    @Test
    void route_null_nest_pas_public() {
        assertFalse(filtre.estRoutePublique(null));
    }

    // --- Tests pour construireJson401() ---

    @Test
    void json_401_contient_code_et_message() {
        String json = filtre.construireJson401();
        assertTrue(json.contains("UNAUTHORIZED"));
        assertTrue(json.contains("Connexion requise"));
    }
}
