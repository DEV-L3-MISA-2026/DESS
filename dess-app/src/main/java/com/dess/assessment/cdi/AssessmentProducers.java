package com.dess.assessment.cdi;

import com.dess.assessment.application.spi.ExamSessionRepository;
import com.dess.assessment.infrastructure.persistence.JpaExamSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @brief CDI producers of the assessment module.
 *
 * <p>Only the repository is produced. The service implementation stays an ordinary bean
 * so its {@code @Transactional} annotation is applied by the container.</p>
 */
@ApplicationScoped
public class AssessmentProducers
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Produces
    @ApplicationScoped
    public ExamSessionRepository examSessionRepository()
    {
        return new JpaExamSessionRepository(entityManager);
    }
}
