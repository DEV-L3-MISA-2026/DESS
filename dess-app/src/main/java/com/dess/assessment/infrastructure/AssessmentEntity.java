package com.dess.assessment.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** @brief JPA entity of the assessment table. */
@Entity
@Table(name = "assessment")
public class AssessmentEntity
{
    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "maximum_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maximumScore;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AssessmentEntity()
    {
    }

    public AssessmentEntity(UUID id, UUID courseId, String title, String type,
                            BigDecimal maximumScore, int durationMinutes, boolean active,
                            OffsetDateTime createdAt)
    {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.type = type;
        this.maximumScore = maximumScore;
        this.durationMinutes = durationMinutes;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId()
    {
        return id;
    }

    public UUID getCourseId()
    {
        return courseId;
    }

    public String getTitle()
    {
        return title;
    }

    public String getType()
    {
        return type;
    }

    public BigDecimal getMaximumScore()
    {
        return maximumScore;
    }

    public int getDurationMinutes()
    {
        return durationMinutes;
    }

    public boolean isActive()
    {
        return active;
    }

    public OffsetDateTime getCreatedAt()
    {
        return createdAt;
    }
}
