package com.dess.security.application;

import com.dess.security.application.impl.LoginThrottle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the failed-login limiter.
 */
class LoginThrottleTest
{
    @Test
    void locksAfterFiveFailures()
    {
        LoginThrottle throttle = new LoginThrottle();
        String key = "admin|127.0.0.1";

        for (int i = 0; i < 4; i++)
        {
            throttle.recordFailure(key);
            assertFalse(throttle.isBlocked(key));
        }

        throttle.recordFailure(key);
        assertTrue(throttle.isBlocked(key));
    }

    @Test
    void successfulLoginClearsTheCounter()
    {
        LoginThrottle throttle = new LoginThrottle();
        String key = "admin|127.0.0.1";

        throttle.recordFailure(key);
        throttle.recordFailure(key);
        throttle.recordSuccess(key);

        for (int i = 0; i < 4; i++)
        {
            throttle.recordFailure(key);
        }

        assertFalse(throttle.isBlocked(key));
    }

    @Test
    void keysAreIndependent()
    {
        LoginThrottle throttle = new LoginThrottle();
        String blocked = "admin|10.0.0.1";
        String other = "admin|10.0.0.2";

        for (int i = 0; i < 5; i++)
        {
            throttle.recordFailure(blocked);
        }

        assertTrue(throttle.isBlocked(blocked));
        assertFalse(throttle.isBlocked(other));
    }
}
