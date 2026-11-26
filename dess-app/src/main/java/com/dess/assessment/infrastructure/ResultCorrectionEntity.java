package com.dess.assessment.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** @brief JPA entity of the assessment result correction table. */
@Entity
@Table(name = "assessment_result_correction")
public class ResultCorrectionEntity
{
    @Id
    private UUID id;

    @Column(name = "result_id", nullable = false)
    private UUID resultId;

    @Column(name = "previous_score", precision = 6, scale = 2)
    private BigDecimal previousScore;

    @Column(name = "previous_status", nullable = false, length = 20)
    private String previousStatus;

    @Column(name = "new_score", precision = 6, scale = 2)
    private BigDecimal newScore;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "corrected_by", nullable = false, length = 200)
    private String correctedBy;

    @Column(name = "corrected_at", nullable = false)
    private OffsetDateTime correctedAt;

    protected ResultCorrectionEntity()
    {
    }

    public ResultCorrectionEntity(UUID id, UUID resultId, BigDecimal previousScore,
                                  String previousStatus, BigDecimal newScore, String newStatus,
                                  String correctedBy, OffsetDateTime correctedAt)
    {
        this.id = id;
        this.resultId = resultId;
        this.previousScore = previousScore;
        this.previousStatus = previousStatus;
        this.newScore = newScore;
        this.newStatus = newStatus;
        this.correctedBy = correctedBy;
        this.correctedAt = correctedAt;
    }

    public UUID getId()
    {
        return id;
    }

    public UUID getResultId()
    {
        return resultId;
    }

    public BigDecimal getPreviousScore()
    {
        return previousScore;
    }

    public String getPreviousStatus()
    {
        return previousStatus;
    }

    public BigDecimal getNewScore()
    {
        return newScore;
    }

    public String getNewStatus()
    {
        return newStatus;
    }

    public String getCorrectedBy()
    {
        return correctedBy;
    }

    public OffsetDateTime getCorrectedAt()
    {
        return correctedAt;
    }
}
