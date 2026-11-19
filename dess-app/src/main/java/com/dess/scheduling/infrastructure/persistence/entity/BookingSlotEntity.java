package com.dess.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_slot", indexes = @Index(name = "booking_slot_booking_idx", columnList = "booking_id"))
public class BookingSlotEntity
{
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(nullable = false, length = 20)
    private String status;

    protected BookingSlotEntity()
    {
    }

    public BookingSlotEntity(UUID id, BookingEntity booking, String resourceType,
                             UUID resourceId, OffsetDateTime startsAt,
                             OffsetDateTime endsAt, String status)
    {
        this.id = id;
        this.booking = booking;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    public UUID getId() { return id; }
    public BookingEntity getBooking() { return booking; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
