package com.dess.academic.cdi;

import com.dess.academic.infrastructure.AcademicRepository;
import com.dess.academic.infrastructure.JpaAcademicRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * CDI producers of the academic module.
 *
 * <p>Only the repositories that are not CDI beans themselves are produced. The service
 * implementations are ordinary beans so their {@code @Transactional} annotation is
 * applied by the container; a bean returned from a producer method is not intercepted.</p>
 */
@ApplicationScoped
public class AcademicProducers
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Produces
    @ApplicationScoped
    public AcademicRepository academicRepository()
    {
        return new JpaAcademicRepository(entityManager);
    }
}
