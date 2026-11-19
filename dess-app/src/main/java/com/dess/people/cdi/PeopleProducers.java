package com.dess.people.cdi;

import com.dess.people.application.PeopleService;
import com.dess.people.application.impl.PeopleServiceImpl;
import com.dess.people.infrastructure.JpaPeopleRepository;
import com.dess.people.infrastructure.PeopleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Producteurs CDI du module people. */
@ApplicationScoped
public class PeopleProducers {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    @Produces
    @ApplicationScoped
    public PeopleService peopleService() {
        PeopleRepository repository = new JpaPeopleRepository(entityManager);
        return new PeopleServiceImpl(repository);
    }
}
