package com.dess.assessment.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** @brief JPA entity of the assessment result table. */
@Entity
@Table(name = "assessment_result")
public class ResultEntity
{
    @Id
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "exam_session_id")
    private UUID examSessionId;

    @Column(name = "student_person_id", nullable = false)
    private long studentPersonId;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "entered_by", nullable = false, length = 200)
    private String enteredBy;

    @Column(name = "entered_at", nullable = false, updatable = false)
    private OffsetDateTime enteredAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ResultEntity()
    {
    }

    public ResultEntity(UUID id, UUID assessmentId, UUID examSessionId, long studentPersonId,
                        BigDecimal score, String status, String enteredBy,
                        OffsetDateTime enteredAt, OffsetDateTime updatedAt)
    {
        this.id = id;
        this.assessmentId = assessmentId;
        this.examSessionId = examSessionId;
        this.studentPersonId = studentPersonId;
        this.score = score;
        this.status = status;
        this.enteredBy = enteredBy;
        this.enteredAt = enteredAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId()
    {
        return id;
    }

    public UUID getAssessmentId()
    {
        return assessmentId;
    }

    public UUID getExamSessionId()
    {
        return examSessionId;
    }

    public long getStudentPersonId()
    {
        return studentPersonId;
    }

    public BigDecimal getScore()
    {
        return score;
    }

    public String getStatus()
    {
        return status;
    }

    public String getEnteredBy()
    {
        return enteredBy;
    }

    public OffsetDateTime getEnteredAt()
    {
        return enteredAt;
    }

    public OffsetDateTime getUpdatedAt()
    {
        return updatedAt;
    }
}
