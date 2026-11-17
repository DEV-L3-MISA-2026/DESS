package mg.dess.platform.concurrency;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Two simultaneous requests for the same room and time: exactly one must succeed. */
class ConcurrentBookingIT {

    @Test
    @Disabled("Enable with the BookingService implementation (plan: Day 8)")
    void onlyOneOfTwoSimultaneousBookingsSucceeds() {
        // Start PostgreSQL with Testcontainers, apply the Flyway migrations, fire two reserve() calls in parallel.
    }
}
