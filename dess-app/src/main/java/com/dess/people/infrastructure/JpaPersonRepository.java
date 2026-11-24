package com.dess.people.infrastructure;

import com.dess.people.application.spi.PersonRepository;
import com.dess.people.domain.Person;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Optional;

/** Implémentation CDI/JPA du port personne. */
@ApplicationScoped
public class JpaPersonRepository implements PersonRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaPersonRepository() {
    }

    public JpaPersonRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Person> findById(long id) {
        return Optional.ofNullable(entityManager.find(AppUserEntity.class, id)).map(this::toPerson);
    }

    @Override
    public Optional<Person> findByEmail(String email) {
        String normalizedEmail = Person.normalizeEmail(email);
        TypedQuery<AppUserEntity> query = entityManager.createQuery(
                "select u from AppUserEntity u where lower(u.email) = :normalizedEmail", AppUserEntity.class);
        query.setParameter("normalizedEmail", normalizedEmail);
        return query.getResultStream().findFirst().map(this::toPerson);
    }

    private Person toPerson(AppUserEntity entity) {
        return new Person(entity.getId(), entity.getEmail(), entity.getLastName(), entity.getFirstName(), entity.isActive());
    }
}
