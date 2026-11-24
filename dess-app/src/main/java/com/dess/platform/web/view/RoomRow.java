package com.dess.platform.web.view;

import java.util.Objects;

/** One room row on the scheduling page. */
public final class RoomRow
{
    private final String code;
    private final String name;
    private final int capacity;

    /**
     * @param code room code
     * @param name room name
     * @param capacity number of seats
     */
    public RoomRow(String code, String name, int capacity)
    {
        this.code = Objects.requireNonNull(code, "code is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.capacity = capacity;
    }

    public String getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }

    public int getCapacity()
    {
        return capacity;
    }
}
