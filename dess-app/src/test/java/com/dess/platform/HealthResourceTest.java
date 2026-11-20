package com.dess.platform;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test unitaire simple pour l'endpoint HealthResource.
 */
class HealthResourceTest {

    @Test
    void healthReturnsStatusUp() {
        HealthResource resource = new HealthResource();
        Map<String, String> response = resource.check();
        assertEquals("UP", response.get("status"));
    }
}
