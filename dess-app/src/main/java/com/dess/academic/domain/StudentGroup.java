package com.dess.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Cohort taught and examined together within an academic period. */
@Entity
@Table(name = "student_group")
public class StudentGroup {
    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "owning_unit_id", nullable = false)
    private UUID owningUnitId;

    @Column(name = "academic_period_id", nullable = false)
    private UUID academicPeriodId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudentGroup() {}

    public StudentGroup(String code, String name, UUID owningUnitId, UUID academicPeriodId) {
        this(UUID.randomUUID(), requireText(code, "Le code du groupe est obligatoire"),
                requireText(name, "Le nom du groupe est obligatoire"),
                Objects.requireNonNull(owningUnitId, "owningUnitId"),
                Objects.requireNonNull(academicPeriodId, "academicPeriodId"),
                true, Instant.now());
    }

    public StudentGroup(UUID id, String code, String name, UUID owningUnitId,
                        UUID academicPeriodId, boolean active, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = requireText(code, "Le code du groupe est obligatoire").trim();
        this.name = requireText(name, "Le nom du groupe est obligatoire").trim();
        this.owningUnitId = Objects.requireNonNull(owningUnitId, "owningUnitId");
        this.academicPeriodId = Objects.requireNonNull(academicPeriodId, "academicPeriodId");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public void rename(String newName) {
        this.name = requireText(newName, "Le nom du groupe est obligatoire").trim();
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public UUID getOwningUnitId() { return owningUnitId; }
    public UUID getAcademicPeriodId() { return academicPeriodId; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
