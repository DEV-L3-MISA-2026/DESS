package com.dess.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking", indexes = @Index(name = "booking_owner_idx", columnList = "owner_type, owner_id"))
public class BookingEntity
{
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_type", nullable = false, length = 30)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BookingEntity()
    {
    }

    public BookingEntity(UUID id, String ownerType, UUID ownerId,
                         OffsetDateTime startsAt, OffsetDateTime endsAt,
                         String status, OffsetDateTime createdAt)
    {
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getOwnerType() { return ownerType; }
    public UUID getOwnerId() { return ownerId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
