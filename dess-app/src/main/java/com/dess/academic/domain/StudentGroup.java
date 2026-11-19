package com.dess.academic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudentGroup() {
    }

    public StudentGroup(String code, String name, UUID owningUnitId, UUID academicPeriodId) {
        this.id = UUID.randomUUID();
        this.code = normalizeCode(code);
        this.name = requireText(name, "name");
        this.owningUnitId = requireId(owningUnitId, "owningUnitId");
        this.academicPeriodId = requireId(academicPeriodId, "academicPeriodId");
        this.createdAt = Instant.now();
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public void deactivate() {
        this.active = false;
    }

    public static String normalizeCode(String code) {
        return requireText(code, "code").toUpperCase();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static UUID requireId(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public UUID getOwningUnitId() {
        return owningUnitId;
    }

    public UUID getAcademicPeriodId() {
        return academicPeriodId;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StudentGroup that && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
