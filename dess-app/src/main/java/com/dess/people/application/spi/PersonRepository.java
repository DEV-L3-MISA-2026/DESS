package com.dess.people.application.spi;

import com.dess.people.domain.Person;
import java.util.Optional;

/** Port de persistance pour l’agrégat personne. */
public interface PersonRepository {
    Optional<Person> findById(long id);

    Optional<Person> findByEmail(String email);
}
