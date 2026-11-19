package com.dess.people.infrastructure;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import com.dess.people.exception.DuplicatePersonException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

/** Repository JPA du module people. */
public class JpaPeopleRepository implements PeopleRepository {
    private final EntityManager entityManager;

    public JpaPeopleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public StudentProfile createStudent(CreateStudentCommand command) {
        try {
            if (findByEmail(command.email()).isPresent()) {
                throw new DuplicatePersonException("Cet e-mail existe déjà : " + command.email());
            }
            AppUserEntity person = new AppUserEntity(command.email().trim(), command.lastName().trim(), command.firstName().trim(), "STUDENT");
            entityManager.persist(person);
            entityManager.flush();
            StudentEntity student = new StudentEntity(person, command.registrationNumber().trim());
            entityManager.persist(student);
            entityManager.flush();
            return new StudentProfile(person.getId(), student.getRegistrationNumber());
        } catch (PersistenceException e) {
            throw new DuplicatePersonException("L'e-mail ou le matricule existe déjà", e);
        }
    }

    @Override
    public TeacherProfile createTeacher(CreateTeacherCommand command) {
        try {
            if (findByEmail(command.email()).isPresent()) {
                throw new DuplicatePersonException("Cet e-mail existe déjà : " + command.email());
            }
            AppUserEntity person = new AppUserEntity(command.email().trim(), command.lastName().trim(), command.firstName().trim(), "TEACHER");
            entityManager.persist(person);
            entityManager.flush();
            TeacherEntity teacher = new TeacherEntity(person, command.academicRank().trim());
            entityManager.persist(teacher);
            entityManager.flush();
            return new TeacherProfile(person.getId(), teacher.getAcademicRank());
        } catch (PersistenceException e) {
            throw new DuplicatePersonException("Cet e-mail existe déjà", e);
        }
    }

    @Override
    public Optional<Person> findById(long id) {
        AppUserEntity entity = entityManager.find(AppUserEntity.class, id);
        return Optional.ofNullable(entity).map(this::toPerson);
    }

    @Override
    public Optional<Person> findByEmail(String email) {
        TypedQuery<AppUserEntity> query = entityManager.createQuery("select u from AppUserEntity u where u.email = :email", AppUserEntity.class);
        query.setParameter("email", email);
        return query.getResultStream().findFirst().map(this::toPerson);
    }

    @Override
    public List<StudentProfile> listStudents() {
        TypedQuery<StudentEntity> query = entityManager.createQuery("select s from StudentEntity s", StudentEntity.class);
        return query.getResultList().stream().map(s -> new StudentProfile(s.getId(), s.getRegistrationNumber())).toList();
    }

    @Override
    public boolean studentExists(long id) {
        Long count = entityManager.createQuery("select count(s) from StudentEntity s where s.id = :id", Long.class)
                .setParameter("id", id).getSingleResult();
        return count != null && count > 0;
    }

    private Person toPerson(AppUserEntity entity) {
        return new Person(entity.getId(), entity.getEmail(), entity.getLastName(), entity.getFirstName(), entity.isActive());
    }
}
