package com.dess.assessment.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;

/** @brief CDI and JPA implementation of the assessment persistence port. */
@ApplicationScoped
public class JpaAssessmentRepository implements AssessmentRepository
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaAssessmentRepository()
    {
    }

    public JpaAssessmentRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public Assessment save(Assessment assessment)
    {
        // Merge covers the creation and the update of an assessment.
        return toDomain(entityManager.merge(toEntity(assessment)));
    }

    @Override
    public Optional<Assessment> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(AssessmentEntity.class, id))
                .map(this::toDomain);
    }

    @Override
    public List<Assessment> findAll()
    {
        return entityManager.createQuery(
                        "select a from AssessmentEntity a order by a.createdAt desc",
                        AssessmentEntity.class)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Assessment> findByCourseId(UUID courseId)
    {
        return entityManager.createQuery(
                        "select a from AssessmentEntity a where a.courseId = :courseId "
                                + "order by a.createdAt desc",
                        AssessmentEntity.class)
                .setParameter("courseId", courseId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    private AssessmentEntity toEntity(Assessment assessment)
    {
        return new AssessmentEntity(assessment.id(), assessment.courseId(), assessment.title(),
                assessment.type().name(), assessment.maximumScore(),
                assessment.durationMinutes(), assessment.active(), assessment.createdAt());
    }

    private Assessment toDomain(AssessmentEntity entity)
    {
        return new Assessment(entity.getId(), entity.getCourseId(), entity.getTitle(),
                AssessmentType.valueOf(entity.getType()), entity.getMaximumScore(),
                entity.getDurationMinutes(), entity.isActive(), entity.getCreatedAt());
    }
}
