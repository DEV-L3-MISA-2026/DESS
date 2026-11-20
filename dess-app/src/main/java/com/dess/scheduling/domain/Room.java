package com.dess.scheduling.domain;

import java.util.Objects;
import java.util.UUID;

/** @brief A schedulable room. */
public record Room(UUID id, String code, String name, int capacity)
{
    public Room
    {
        Objects.requireNonNull(id, "id is required");
        if (code == null || code.isBlank())
        {
            throw new IllegalArgumentException("code is required");
        }
        if (code.length() > 50)
        {
            throw new IllegalArgumentException("code must contain at most 50 characters");
        }
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > 200)
        {
            throw new IllegalArgumentException("name must contain at most 200 characters");
        }
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
    }

    public static Room create(String code, String name, int capacity)
    {
        return new Room(UUID.randomUUID(), code, name, capacity);
    }
}
