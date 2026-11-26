package com.dess.people.infrastructure;

import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.spi.TeacherRepository;
import com.dess.people.domain.TeacherProfile;
import com.dess.people.domain.Person;
import com.dess.people.exception.DuplicateEmailException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

/** Implémentation JPA du port enseignant. */
@ApplicationScoped
public class JpaTeacherRepository implements TeacherRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaTeacherRepository() {
    }

    public JpaTeacherRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public TeacherProfile create(CreateTeacherCommand command) {
        String email = command.email().trim();
        String normalizedEmail = Person.normalizeEmail(email);
        Long count = entityManager.createQuery(
                        "select count(u) from AppUserEntity u where lower(u.email) = :normalizedEmail", Long.class)
                .setParameter("normalizedEmail", normalizedEmail)
                .getSingleResult();
        if (count != null && count > 0) throw new DuplicateEmailException(email);

        try {
            AppUserEntity person = new AppUserEntity(email, command.lastName().trim(), command.firstName().trim(), "TEACHER");
            entityManager.persist(person);
            entityManager.flush();

            TeacherEntity teacher = new TeacherEntity(person, command.academicRank().trim());
            entityManager.persist(teacher);
            entityManager.flush();
            return new TeacherProfile(person.getId(), teacher.getAcademicRank());
        } catch (PersistenceException exception) {
            throw new DuplicateEmailException(email, exception);
        }
    }
}
