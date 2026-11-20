package com.dess.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "room")
public class RoomEntity
{
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private int capacity;

    protected RoomEntity()
    {
    }

    public RoomEntity(UUID id, String code, String name, int capacity)
    {
        this.id = id;
        this.code = code;
        this.name = name;
        this.capacity = capacity;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }

    public void update(String name, int capacity)
    {
        this.name = name;
        this.capacity = capacity;
    }
}
