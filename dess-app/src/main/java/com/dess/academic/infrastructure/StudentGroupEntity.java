package com.dess.academic.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Entité JPA de la table student_group. */
@Entity
@Table(name = "student_group")
public class StudentGroupEntity
{
    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "owning_unit_id", nullable = false, length = 100)
    private String owningUnitId;

    @Column(name = "academic_period_id", nullable = false)
    private long academicPeriodId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudentGroupEntity() {}

    public StudentGroupEntity(UUID id, String code, String name,
                              String owningUnitId, long academicPeriodId,
                              boolean active, Instant createdAt)
    {
        this.id = id;
        this.code = code;
        this.name = name;
        this.owningUnitId = owningUnitId;
        this.academicPeriodId = academicPeriodId;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getOwningUnitId() { return owningUnitId; }
    public long getAcademicPeriodId() { return academicPeriodId; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
