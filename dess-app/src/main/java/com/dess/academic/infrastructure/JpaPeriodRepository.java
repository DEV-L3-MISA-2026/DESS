package com.dess.academic.infrastructure;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.spi.PeriodRepository;
import com.dess.academic.domain.AcademicPeriod;
import com.dess.academic.exception.DuplicatePeriodException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;

/** Implémentation JPA du port des périodes. */
@ApplicationScoped
public class JpaPeriodRepository implements PeriodRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaPeriodRepository() {}

    public JpaPeriodRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public AcademicPeriod create(CreatePeriodCommand command) {
        AcademicPeriod domain = new AcademicPeriod(null, command.academicYear(), command.term());
        try {
            PeriodEntity entity = new PeriodEntity(domain.academicYear(), domain.term());
            entityManager.persist(entity);
            entityManager.flush();
            return new AcademicPeriod(entity.getId(), entity.getAcademicYear(), entity.getTerm());
        } catch (PersistenceException exception) {
            throw new DuplicatePeriodException("Cette période académique existe déjà", exception);
        }
    }

    @Override
    public List<AcademicPeriod> findAll() {
        return entityManager.createQuery("select p from PeriodEntity p", PeriodEntity.class)
                .getResultList().stream()
                .map(entity -> new AcademicPeriod(entity.getId(), entity.getAcademicYear(), entity.getTerm()))
                .toList();
    }
}
