package com.dess.assessment.infrastructure;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.dess.assessment.application.spi.ResultCorrectionRepository;
import com.dess.assessment.domain.ResultCorrection;
import com.dess.assessment.domain.ResultStatus;

/** @brief CDI and JPA implementation of the correction history port. */
@ApplicationScoped
public class JpaResultCorrectionRepository implements ResultCorrectionRepository
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaResultCorrectionRepository()
    {
    }

    public JpaResultCorrectionRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public ResultCorrection save(ResultCorrection correction)
    {
        // Merge covers the creation of a correction entry. Entries are
        // never updated: a correction is history, not a live row.
        return toDomain(entityManager.merge(toEntity(correction)));
    }

    @Override
    public List<ResultCorrection> findByResultId(UUID resultId)
    {
        return entityManager.createQuery(
                        "select c from ResultCorrectionEntity c where c.resultId = :resultId "
                                + "order by c.correctedAt asc, c.id asc",
                        ResultCorrectionEntity.class)
                .setParameter("resultId", resultId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    private ResultCorrectionEntity toEntity(ResultCorrection correction)
    {
        return new ResultCorrectionEntity(correction.id(), correction.resultId(),
                correction.previousScore(), correction.previousStatus().name(),
                correction.newScore(), correction.newStatus().name(),
                correction.correctedBy(), correction.correctedAt());
    }

    private ResultCorrection toDomain(ResultCorrectionEntity entity)
    {
        return new ResultCorrection(entity.getId(), entity.getResultId(),
                entity.getPreviousScore(), ResultStatus.valueOf(entity.getPreviousStatus()),
                entity.getNewScore(), ResultStatus.valueOf(entity.getNewStatus()),
                entity.getCorrectedBy(), entity.getCorrectedAt());
    }
}
