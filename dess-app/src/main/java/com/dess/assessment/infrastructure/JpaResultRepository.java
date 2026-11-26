package com.dess.assessment.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.dess.assessment.application.spi.ResultRepository;
import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultStatus;

/** @brief CDI and JPA implementation of the result persistence port. */
@ApplicationScoped
public class JpaResultRepository implements ResultRepository
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaResultRepository()
    {
    }

    public JpaResultRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public Result save(Result result)
    {
        // Merge covers the creation and the correction of a result.
        return toDomain(entityManager.merge(toEntity(result)));
    }

    @Override
    public Optional<Result> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(ResultEntity.class, id))
                .map(this::toDomain);
    }

    @Override
    public Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId)
    {
        return entityManager.createQuery(
                        "select r from ResultEntity r where r.assessmentId = :assessmentId "
                                + "and r.studentPersonId = :studentPersonId",
                        ResultEntity.class)
                .setParameter("assessmentId", assessmentId)
                .setParameter("studentPersonId", studentPersonId)
                .getResultStream().findFirst().map(this::toDomain);
    }

    @Override
    public List<Result> findByAssessment(UUID assessmentId)
    {
        return entityManager.createQuery(
                        "select r from ResultEntity r where r.assessmentId = :assessmentId "
                                + "order by r.enteredAt",
                        ResultEntity.class)
                .setParameter("assessmentId", assessmentId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Result> findByStudent(long studentPersonId)
    {
        return entityManager.createQuery(
                        "select r from ResultEntity r where r.studentPersonId = :studentPersonId "
                                + "order by r.enteredAt",
                        ResultEntity.class)
                .setParameter("studentPersonId", studentPersonId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByAssessmentAndStudent(UUID assessmentId, long studentPersonId)
    {
        return entityManager.createQuery(
                        "select count(r) from ResultEntity r where r.assessmentId = :assessmentId "
                                + "and r.studentPersonId = :studentPersonId",
                        Long.class)
                .setParameter("assessmentId", assessmentId)
                .setParameter("studentPersonId", studentPersonId)
                .getSingleResult() > 0;
    }

    private ResultEntity toEntity(Result result)
    {
        return new ResultEntity(result.id(), result.assessmentId(), result.examSessionId(),
                result.studentPersonId(), result.score(), result.status().name(),
                result.enteredBy(), result.enteredAt(), result.updatedAt());
    }

    private Result toDomain(ResultEntity entity)
    {
        return new Result(entity.getId(), entity.getAssessmentId(), entity.getExamSessionId(),
                entity.getStudentPersonId(), entity.getScore(),
                ResultStatus.valueOf(entity.getStatus()), entity.getEnteredBy(),
                entity.getEnteredAt(), entity.getUpdatedAt());
    }
}
