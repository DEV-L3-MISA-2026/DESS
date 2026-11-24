package com.dess.people.infrastructure;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.spi.StudentRepository;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.exception.DuplicateEmailException;
import com.dess.people.exception.DuplicateRegistrationNumberException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.util.List;

/** Implémentation JPA du port étudiant. */
@ApplicationScoped
public class JpaStudentRepository implements StudentRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaStudentRepository() {
    }

    public JpaStudentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public StudentProfile create(CreateStudentCommand command) {
        String email = command.email().trim();
        String normalizedEmail = Person.normalizeEmail(email);
        String registrationNumber = command.registrationNumber().trim();

        if (emailExists(normalizedEmail)) throw new DuplicateEmailException(email);
        if (registrationNumberExists(registrationNumber)) {
            throw new DuplicateRegistrationNumberException(registrationNumber);
        }

        try {
            AppUserEntity person = new AppUserEntity(email, command.lastName().trim(), command.firstName().trim(), "STUDENT");
            entityManager.persist(person);
            entityManager.flush();

            StudentEntity student = new StudentEntity(person, registrationNumber);
            entityManager.persist(student);
            entityManager.flush();
            return new StudentProfile(person.getId(), student.getRegistrationNumber());
        } catch (PersistenceException exception) {
            throw translateUniquenessViolation(exception);
        }
    }

    @Override
    public List<StudentProfile> findAll() {
        TypedQuery<StudentEntity> query = entityManager.createQuery(
                "select s from StudentEntity s", StudentEntity.class);
        return query.getResultList().stream()
                .map(student -> new StudentProfile(student.getId(), student.getRegistrationNumber()))
                .toList();
    }

    @Override
    public boolean exists(long id) {
        Long count = entityManager.createQuery(
                        "select count(s) from StudentEntity s where s.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        return count != null && count > 0;
    }

    private boolean emailExists(String normalizedEmail) {
        Long count = entityManager.createQuery(
                        "select count(u) from AppUserEntity u where lower(u.email) = :normalizedEmail", Long.class)
                .setParameter("normalizedEmail", normalizedEmail)
                .getSingleResult();
        return count != null && count > 0;
    }

    private boolean registrationNumberExists(String registrationNumber) {
        Long count = entityManager.createQuery(
                        "select count(s) from StudentEntity s where s.registrationNumber = :registrationNumber", Long.class)
                .setParameter("registrationNumber", registrationNumber)
                .getSingleResult();
        return count != null && count > 0;
    }

    private RuntimeException translateUniquenessViolation(PersistenceException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String detail = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
        if (detail.contains("registration_number") || detail.contains("uq_student_registration_number")
                || detail.contains("matricule")) {
            return new DuplicateRegistrationNumberException(detail);
        }
        return new DuplicateEmailException(detail, exception);
    }
}
