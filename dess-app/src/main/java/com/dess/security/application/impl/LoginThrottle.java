package com.dess.security.application.impl;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits repeated failed logins.
 *
 * <p>Five failures in five minutes lock the key for five minutes. The key is normally
 * the username plus the client address, so one account does not lock every caller. The
 * state is in memory: it resets on redeploy and is not shared between nodes.</p>
 */
@ApplicationScoped
public class LoginThrottle
{
    static final int MAX_FAILURES = 5;
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(5);
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginThrottle()
    {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock clock)
    {
        this.clock = clock;
    }

    /**
     * @param key login key, usually username and client address
     * @return true while the key is locked
     */
    public boolean isBlocked(String key)
    {
        Attempt attempt = attempts.get(key);

        return attempt != null
                && attempt.lockedUntil != null
                && clock.instant().isBefore(attempt.lockedUntil);
    }

    /**
     * Records a failed login.
     *
     * @param key login key
     */
    public void recordFailure(String key)
    {
        Instant now = clock.instant();

        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.firstFailure.plus(FAILURE_WINDOW).isBefore(now))
            {
                return new Attempt(1, now, null);
            }

            int failures = current.failures + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;

            return new Attempt(failures, current.firstFailure, lockedUntil);
        });
    }

    /**
     * Clears the failure count after a successful login.
     *
     * @param key login key
     */
    public void recordSuccess(String key)
    {
        attempts.remove(key);
    }

    private record Attempt(int failures, Instant firstFailure, Instant lockedUntil)
    {
    }
}
