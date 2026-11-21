package com.dess.assessment.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * @brief JPA mapping of one exam session.
 *
 * <p>The tables are created by Flyway. The element collections keep the numeric
 * person ids in insertion order so a stored session equals the domain value.</p>
 */
@Entity
@Table(name = "exam_session")
public class ExamSessionEntity
{
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exam_session_candidate",
            joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "person_id", nullable = false)
    private List<Long> candidatePersonIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exam_session_invigilator",
            joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "person_id", nullable = false)
    private List<Long> invigilatorPersonIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ExamSessionEntity()
    {
    }

    public ExamSessionEntity(UUID id, UUID assessmentId, UUID roomId,
                             OffsetDateTime startsAt, OffsetDateTime endsAt,
                             List<Long> candidatePersonIds, List<Long> invigilatorPersonIds,
                             OffsetDateTime createdAt)
    {
        this.id = id;
        this.assessmentId = assessmentId;
        this.roomId = roomId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.candidatePersonIds = new ArrayList<>(candidatePersonIds);
        this.invigilatorPersonIds = new ArrayList<>(invigilatorPersonIds);
        this.createdAt = createdAt;
    }

    public void update(UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt,
                       List<Long> candidatePersonIds, List<Long> invigilatorPersonIds)
    {
        this.roomId = roomId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.candidatePersonIds.clear();
        this.candidatePersonIds.addAll(candidatePersonIds);
        this.invigilatorPersonIds.clear();
        this.invigilatorPersonIds.addAll(invigilatorPersonIds);
    }

    public UUID getId() { return id; }
    public UUID getAssessmentId() { return assessmentId; }
    public UUID getRoomId() { return roomId; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public List<Long> getCandidatePersonIds() { return candidatePersonIds; }
    public List<Long> getInvigilatorPersonIds() { return invigilatorPersonIds; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
