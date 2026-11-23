package com.dess.security.infrastructure.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the PBKDF2 encoder.
 */
class Pbkdf2PasswordEncoderTest
{
    private final Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder();

    @Test
    void hashesAndMatchesPasswordCorrectly()
    {
        String hash = encoder.encode("MonMotDePasse123");

        assertNotNull(hash);
        assertTrue(hash.startsWith("pbkdf2-sha256$"));
        assertTrue(encoder.matches("MonMotDePasse123", hash));
        assertFalse(encoder.matches("MauvaisMotDePasse", hash));
    }

    @Test
    void usesANewSaltForEveryHash()
    {
        String first = encoder.encode("same-password");
        String second = encoder.encode("same-password");

        assertNotEquals(first, second);
        assertTrue(encoder.matches("same-password", first));
        assertTrue(encoder.matches("same-password", second));
    }

    @Test
    void handlesNullAndMalformedInput()
    {
        assertFalse(encoder.matches(null, "someHash"));
        assertFalse(encoder.matches("password", null));
        assertFalse(encoder.matches("password", "not-a-valid-format"));
        assertFalse(encoder.matches("password", "pbkdf2-sha256$bad$bad$bad"));
    }

    @Test
    void rejectsANullPasswordWhenEncoding()
    {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(null));
    }
}
