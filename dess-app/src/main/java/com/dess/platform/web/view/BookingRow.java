package com.dess.platform.web.view;

import java.util.Objects;

/** One booking row on the overview and scheduling pages. */
public final class BookingRow
{
    private final String ownerType;
    private final String resources;
    private final String startsAt;
    private final String endsAt;
    private final Badge status;

    /**
     * @param ownerType owner label
     * @param resources reserved room codes
     * @param startsAt formatted UTC start
     * @param endsAt formatted UTC end
     * @param active whether the booking is still active
     */
    public BookingRow(String ownerType, String resources, String startsAt, String endsAt,
                      boolean active)
    {
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType is required");
        this.resources = Objects.requireNonNull(resources, "resources is required");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt is required");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt is required");
        this.status = Badge.booking(active);
    }

    public String getOwnerType()
    {
        return ownerType;
    }

    public String getResources()
    {
        return resources;
    }

    public String getStartsAt()
    {
        return startsAt;
    }

    public String getEndsAt()
    {
        return endsAt;
    }

    public Badge getStatus()
    {
        return status;
    }
}
