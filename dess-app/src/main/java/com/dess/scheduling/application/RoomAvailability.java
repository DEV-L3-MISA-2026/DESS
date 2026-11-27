package com.dess.scheduling.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/** @brief The taken and free intervals of one room for one day. */
public record RoomAvailability(List<Interval> taken, List<Interval> free)
{
    public RoomAvailability
    {
        Objects.requireNonNull(taken, "taken is required");
        Objects.requireNonNull(free, "free is required");
    }

    /** @brief A half-open time range: [startsAt, endsAt). */
    public record Interval(OffsetDateTime startsAt, OffsetDateTime endsAt)
    {
        public Interval
        {
            Objects.requireNonNull(startsAt, "startsAt is required");
            Objects.requireNonNull(endsAt, "endsAt is required");
            if (!startsAt.isBefore(endsAt))
            {
                throw new IllegalArgumentException("startsAt must be before endsAt");
            }
        }
    }
}
