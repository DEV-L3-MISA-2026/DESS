package com.dess.security.web;

import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates and checks the per-session CSRF token.
 *
 * <p>Forms send the token in the {@code csrfToken} field; scripts send it in the
 * {@code X-CSRF-Token} header. The comparison is constant time.</p>
 */
public final class CsrfTokens
{
    public static final String SESSION_ATTRIBUTE = "dess.csrfToken";
    public static final String REQUEST_ATTRIBUTE = "csrfToken";
    public static final String PARAMETER = "csrfToken";
    public static final String HEADER = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokens()
    {
    }

    /**
     * Returns the token of the session, creating it on first use.
     *
     * @param session HTTP session
     * @return the token
     */
    public static String ensure(HttpSession session)
    {
        if (session == null)
        {
            throw new IllegalArgumentException("session is required");
        }

        Object existing = session.getAttribute(SESSION_ATTRIBUTE);

        if (existing instanceof String token && !token.isBlank())
        {
            return token;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, token);

        return token;
    }

    /**
     * Checks a supplied token against the session token.
     *
     * @param session HTTP session, may be {@code null}
     * @param supplied token from the form or header
     * @return true when both values are present and equal
     */
    public static boolean matches(HttpSession session, String supplied)
    {
        if (session == null || supplied == null)
        {
            return false;
        }

        Object expected = session.getAttribute(SESSION_ATTRIBUTE);

        if (!(expected instanceof String expectedToken) || supplied.isBlank())
        {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
