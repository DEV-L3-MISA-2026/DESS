package com.dess.assessment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.application.spi.ExamSessionRepository;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.infrastructure.persistence.entity.ExamSessionEntity;
import jakarta.persistence.EntityManager;

/**
 * @brief JPA adapter of the exam session repository port.
 */
public class JpaExamSessionRepository implements ExamSessionRepository
{
    private final EntityManager entityManager;

    public JpaExamSessionRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public ExamSession save(ExamSession session)
    {
        ExamSessionEntity entity = entityManager.find(ExamSessionEntity.class, session.id());

        if (entity == null)
        {
            entity = new ExamSessionEntity(session.id(), session.assessmentId(), session.roomId(),
                    session.startsAt(), session.endsAt(), session.candidatePersonIds(),
                    session.invigilatorPersonIds(), session.createdAt());
            entityManager.persist(entity);
        }
        else
        {
            entity.update(session.roomId(), session.startsAt(), session.endsAt(),
                    session.candidatePersonIds(), session.invigilatorPersonIds());
        }

        return toDomain(entity);
    }

    @Override
    public Optional<ExamSession> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(ExamSessionEntity.class, id))
                .map(this::toDomain);
    }

    @Override
    public List<ExamSession> findAll()
    {
        return entityManager.createQuery(
                        "select s from ExamSessionEntity s order by s.startsAt desc",
                        ExamSessionEntity.class)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id)
    {
        ExamSessionEntity entity = entityManager.find(ExamSessionEntity.class, id);

        if (entity != null)
        {
            entityManager.remove(entity);
        }
    }

    private ExamSession toDomain(ExamSessionEntity entity)
    {
        return new ExamSession(entity.getId(), entity.getAssessmentId(), entity.getRoomId(),
                entity.getStartsAt(), entity.getEndsAt(), entity.getCandidatePersonIds(),
                entity.getInvigilatorPersonIds(), entity.getCreatedAt());
    }
}
