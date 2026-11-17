package mg.dess.shared.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimeRangeTest {

    private static TimeRange range(String from, String to) {
        return new TimeRange(Instant.parse(from), Instant.parse(to));
    }

    @Test
    void adjacentRangesDoNotOverlap() {
        var morning = range("2026-11-17T08:00:00Z", "2026-11-17T10:00:00Z");
        var next = range("2026-11-17T10:00:00Z", "2026-11-17T12:00:00Z");
        assertFalse(morning.overlaps(next));
        assertFalse(next.overlaps(morning));
    }

    @Test
    void intersectingRangesOverlap() {
        var a = range("2026-11-17T08:00:00Z", "2026-11-17T10:00:00Z");
        var b = range("2026-11-17T09:00:00Z", "2026-11-17T11:00:00Z");
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void startMustBeBeforeEnd() {
        var t = Instant.parse("2026-11-17T08:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new TimeRange(t, t));
    }
}
