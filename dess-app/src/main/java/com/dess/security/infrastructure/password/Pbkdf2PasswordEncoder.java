package com.dess.security.infrastructure.password;

import com.dess.security.application.spi.PasswordEncoder;
import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PBKDF2 password encoder.
 *
 * <p>Each encoded value has the form
 * {@code pbkdf2-sha256$iterations$base64-salt$base64-hash}. The salt is random
 * for every call, so two encodings of the same password differ. Verification
 * recomputes the hash with the stored salt and compares the bytes in constant
 * time.</p>
 */
@ApplicationScoped
public class Pbkdf2PasswordEncoder implements PasswordEncoder
{
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int DEFAULT_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public Pbkdf2PasswordEncoder()
    {
        this(DEFAULT_ITERATIONS);
    }

    Pbkdf2PasswordEncoder(int iterations)
    {
        if (iterations < 1)
        {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    @Override
    public String encode(String rawPassword)
    {
        if (rawPassword == null)
        {
            throw new IllegalArgumentException("password is required");
        }

        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = hash(rawPassword.toCharArray(), salt, iterations);
        Base64.Encoder encoder = Base64.getEncoder();

        return PREFIX + "$" + iterations + "$"
                + encoder.encodeToString(salt) + "$"
                + encoder.encodeToString(hash);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword)
    {
        if (rawPassword == null || encodedPassword == null)
        {
            return false;
        }

        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0]))
        {
            return false;
        }

        try
        {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = hash(rawPassword.toCharArray(), salt, storedIterations);

            return MessageDigest.isEqual(expected, actual);
        }
        catch (IllegalArgumentException exception)
        {
            return false;
        }
    }

    private byte[] hash(char[] password, byte[] salt, int rounds)
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, HASH_BITS);

        try
        {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException exception)
        {
            throw new IllegalStateException("PBKDF2 is not available", exception);
        }
        finally
        {
            spec.clearPassword();
        }
    }
}
