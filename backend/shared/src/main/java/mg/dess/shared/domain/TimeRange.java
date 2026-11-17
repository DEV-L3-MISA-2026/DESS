package mg.dess.shared.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Half-open time range [start, end). Instants are expressed in UTC; the institution's
 * time zone is applied only when displaying.
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end: " + start + " / " + end);
        }
    }

    /** A range ending at 10:00 does not overlap a range starting at 10:00. */
    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
