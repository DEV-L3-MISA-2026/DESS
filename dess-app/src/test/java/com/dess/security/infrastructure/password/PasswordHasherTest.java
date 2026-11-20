package com.dess.security.infrastructure.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test unitaire pour PasswordHasher.
 */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesAndMatchesPasswordCorrectly() {
        String rawPassword = "MonMotDePasse123";
        String hash = hasher.hash(rawPassword);

        assertNotNull(hash);
        assertTrue(hasher.matches(rawPassword, hash));
        assertFalse(hasher.matches("MauvaisMotDePasse", hash));
    }

    @Test
    void handlesNullInputsGracefully() {
        assertFalse(hasher.matches(null, "someHash"));
        assertFalse(hasher.matches("password", null));
    }
}
