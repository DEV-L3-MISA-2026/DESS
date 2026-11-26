package com.dess.academic.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Entité JPA de la table group_enrollment. */
@Entity
@Table(name = "group_enrollment")
public class GroupEnrollmentEntity
{
    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "academic_period_id", nullable = false)
    private UUID academicPeriodId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    protected GroupEnrollmentEntity() {}

    public GroupEnrollmentEntity(UUID id, UUID studentId, UUID groupId,
                                 UUID academicPeriodId, Instant enrolledAt)
    {
        this.id = id;
        this.studentId = studentId;
        this.groupId = groupId;
        this.academicPeriodId = academicPeriodId;
        this.enrolledAt = enrolledAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getGroupId() { return groupId; }
    public UUID getAcademicPeriodId() { return academicPeriodId; }
    public Instant getEnrolledAt() { return enrolledAt; }
}
