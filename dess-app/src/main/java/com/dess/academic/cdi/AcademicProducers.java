package com.dess.academic.cdi;

import com.dess.academic.application.PeriodService;
import com.dess.academic.application.ProgramEnrollmentService;
import com.dess.academic.application.ProgramService;
import com.dess.academic.application.impl.PeriodServiceImpl;
import com.dess.academic.application.impl.ProgramEnrollmentServiceImpl;
import com.dess.academic.application.impl.ProgramServiceImpl;
import com.dess.academic.infrastructure.AcademicRepository;
import com.dess.academic.infrastructure.JpaAcademicRepository;
import com.dess.people.application.PeopleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Producteurs CDI du module academic. */
@ApplicationScoped
public class AcademicProducers {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Inject
    private PeopleService peopleService;

    private AcademicRepository repository() {
        return new JpaAcademicRepository(entityManager);
    }

    @Produces
    @ApplicationScoped
    public PeriodService periodService() {
        return new PeriodServiceImpl(repository());
    }

    @Produces
    @ApplicationScoped
    public ProgramService programService() {
        return new ProgramServiceImpl(repository());
    }

    @Produces
    @ApplicationScoped
    public ProgramEnrollmentService programEnrollmentService() {
        return new ProgramEnrollmentServiceImpl(repository(), peopleService);
    }
}
