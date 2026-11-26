package com.dess.people.application;

import com.dess.people.application.impl.PeopleServiceImpl;
import com.dess.people.application.spi.PersonRepository;
import com.dess.people.application.spi.StudentRepository;
import com.dess.people.application.spi.TeacherRepository;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import com.dess.people.exception.DuplicateEmailException;
import com.dess.people.exception.DuplicateRegistrationNumberException;
import com.dess.people.exception.PersonNotFoundException;
import com.dess.people.exception.StudentValidationException;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests unitaires du service people avec ports en mémoire. */
class PeopleServiceImplTest {
    @Test
    void createsAndFindsStudent() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);

        StudentProfile student = service.createStudent(
                new CreateStudentCommand("student@dess.mg", "Rakoto", "Jean", "ETU001"));

        assertTrue(service.studentExists(student.personId()));
        assertEquals("student@dess.mg", service.findByEmail("student@dess.mg").orElseThrow().email());
        assertEquals(List.of(student), service.listStudents());
    }

    @Test
    void createsTeacher() {
        PeopleService service = service(new InMemoryPeopleRepositories());
        TeacherProfile teacher = service.createTeacher(
                new CreateTeacherCommand("teacher@dess.mg", "Rabe", "Paul", "Professeur"));

        assertEquals("Professeur", teacher.academicRank());
    }

    @Test
    void rejectsDuplicateEmail() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);
        service.createStudent(new CreateStudentCommand("same@dess.mg", "Rakoto", "Jean", "ETU001"));

        assertThrows(DuplicateEmailException.class,
                () -> service.createTeacher(new CreateTeacherCommand("same@dess.mg", "Rabe", "Paul", "Professeur")));
    }

    @Test
    void rejectsDuplicateRegistrationNumber() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);
        service.createStudent(new CreateStudentCommand("one@dess.mg", "Rakoto", "Jean", "ETU001"));

        assertThrows(DuplicateRegistrationNumberException.class,
                () -> service.createStudent(new CreateStudentCommand("two@dess.mg", "Rabe", "Paul", "ETU001")));
    }

    @Test
    void rejectsNullAndBlankCommands() {
        PeopleService service = service(new InMemoryPeopleRepositories());

        assertThrows(IllegalArgumentException.class, () -> service.createStudent(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.createStudent(new CreateStudentCommand(" ", "Rakoto", "Jean", "ETU001")));
        assertThrows(IllegalArgumentException.class,
                () -> service.findByEmail(" "));
    }

    @Test
    void rejectsCaseDifferentDuplicateEmailAndPreservesOriginalCase() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);

        StudentProfile student = service.createStudent(
                new CreateStudentCommand("Ada@x", "Rakoto", "Jean", "ETU-CASE-1"));

        assertEquals("Ada@x", service.findByEmail(" ada@x ").orElseThrow().email());
        assertThrows(DuplicateEmailException.class,
                () -> service.createTeacher(new CreateTeacherCommand("ada@x", "Rabe", "Paul", "Professeur")));
        assertThrows(DuplicateEmailException.class,
                () -> service.createStudent(new CreateStudentCommand(" ADA@X ", "Rabe", "Paul", "ETU-CASE-2")));
        assertEquals("Ada@x", repositories.persons.get(student.personId()).email());
    }

    @Test
    void updatesStudentAndKeepsOriginalEmailCase() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);
        StudentProfile student = service.createStudent(
                new CreateStudentCommand("Ada@x", "Rakoto", "Jean", "ETU-UPDATE-1"));

        Person updated = service.updateStudent(student.personId(),
                new UpdateStudentCommand("Ada@Example.Mg", "Rabe", "Paul"));

        assertEquals("Ada@Example.Mg", updated.email());
        assertEquals("Rabe", updated.lastName());
        assertEquals("Paul", updated.firstName());
        assertEquals(updated, service.findById(student.personId()).orElseThrow());
    }

    @Test
    void rejectsAllStudentFieldViolationsInOneSummary() {
        PeopleService service = service(new InMemoryPeopleRepositories());

        StudentValidationException exception = assertThrows(StudentValidationException.class,
                () -> service.updateStudent(1L,
                        new UpdateStudentCommand("not-an-email", " ", " ")));

        assertEquals(3, exception.getFieldErrors().size());
        assertTrue(exception.getFieldErrors().containsKey("firstName"));
        assertTrue(exception.getFieldErrors().containsKey("lastName"));
        assertTrue(exception.getFieldErrors().containsKey("email"));
        assertTrue(exception.getMessage().contains("firstName"));
    }

    @Test
    void updateRejectsAnotherStudentsEmailButKeepsOwnEmail() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);
        StudentProfile first = service.createStudent(
                new CreateStudentCommand("first@dess.mg", "One", "Student", "ETU-DUP-1"));
        StudentProfile second = service.createStudent(
                new CreateStudentCommand("Second@dess.mg", "Two", "Student", "ETU-DUP-2"));

        assertThrows(DuplicateEmailException.class,
                () -> service.updateStudent(first.personId(),
                        new UpdateStudentCommand("SECOND@DESS.MG", "One", "Student")));

        Person unchanged = service.updateStudent(second.personId(),
                new UpdateStudentCommand(" second@dess.mg ", "Toto", "Toto"));
        assertEquals("second@dess.mg", unchanged.email());
    }

    @Test
    void deactivationIsIdempotentAndPreservesReadableRecord() {
        InMemoryPeopleRepositories repositories = new InMemoryPeopleRepositories();
        PeopleService service = service(repositories);
        StudentProfile student = service.createStudent(
                new CreateStudentCommand("inactive@dess.mg", "Rakoto", "Jean", "ETU-OFF-1"));

        service.deactivateStudent(student.personId());
        service.deactivateStudent(student.personId());

        assertTrue(service.findById(student.personId()).isPresent());
        assertTrue(service.listStudents().stream()
                .noneMatch(profile -> profile.personId() == student.personId()));
        assertTrue(service.listStudentsIncludingInactive().stream()
                .anyMatch(profile -> profile.personId() == student.personId()));
    }

    private PeopleService service(InMemoryPeopleRepositories repositories) {
        return new PeopleServiceImpl(repositories, repositories, repositories);
    }

    private static class InMemoryPeopleRepositories implements PersonRepository, StudentRepository, TeacherRepository {
        private final Map<Long, Person> persons = new HashMap<>();
        private final Map<Long, StudentProfile> students = new HashMap<>();
        private final Map<String, Long> emails = new HashMap<>();
        private final Map<String, Long> registrationNumbers = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Optional<Person> findById(long id) {
            return Optional.ofNullable(persons.get(id));
        }

        @Override
        public Optional<Person> findByEmail(String email) {
            Long id = emails.get(Person.normalizeEmail(email));
            return id == null ? Optional.empty() : Optional.ofNullable(persons.get(id));
        }

        @Override
        public StudentProfile create(CreateStudentCommand command) {
            String email = command.email().trim();
            String normalizedEmail = Person.normalizeEmail(email);
            if (emails.containsKey(normalizedEmail)) throw new DuplicateEmailException(email);
            String registrationNumber = command.registrationNumber().trim();
            if (registrationNumbers.containsKey(registrationNumber)) {
                throw new DuplicateRegistrationNumberException(registrationNumber);
            }
            long id = nextId++;
            persons.put(id, new Person(id, email, command.lastName(), command.firstName(), true));
            StudentProfile profile = new StudentProfile(id, registrationNumber);
            students.put(id, profile);
            emails.put(normalizedEmail, id);
            registrationNumbers.put(registrationNumber, id);
            return profile;
        }

        @Override
        public List<StudentProfile> findAll() {
            return students.entrySet().stream()
                    .filter(entry -> persons.get(entry.getKey()).active())
                    .map(Map.Entry::getValue)
                    .toList();
        }

        @Override
        public boolean exists(long id) {
            return students.containsKey(id);
        }

        @Override
        public TeacherProfile create(CreateTeacherCommand command) {
            String email = command.email().trim();
            String normalizedEmail = Person.normalizeEmail(email);
            if (emails.containsKey(normalizedEmail)) throw new DuplicateEmailException(email);
            long id = nextId++;
            persons.put(id, new Person(id, email, command.lastName(), command.firstName(), true));
            emails.put(normalizedEmail, id);
            return new TeacherProfile(id, command.academicRank());
        }

        @Override
        public Person update(long id, UpdateStudentCommand command) {
            Person current = persons.get(id);
            if (current == null) throw new com.dess.people.exception.PersonNotFoundException("Étudiant introuvable : " + id);
            String email = command.email().trim();
            String normalizedEmail = Person.normalizeEmail(email);
            Long owner = emails.get(normalizedEmail);
            if (owner != null && owner != id) throw new DuplicateEmailException(email);
            emails.remove(Person.normalizeEmail(current.email()));
            emails.put(normalizedEmail, id);
            Person updated = new Person(id, email, command.lastName().trim(), command.firstName().trim(), current.active());
            persons.put(id, updated);
            return updated;
        }

        @Override
        public void deactivate(long id) {
            Person current = persons.get(id);
            if (current == null) throw new com.dess.people.exception.PersonNotFoundException("Étudiant introuvable : " + id);
            persons.put(id, new Person(id, current.email(), current.lastName(), current.firstName(), false));
        }

        @Override
        public List<StudentProfile> findAllIncludingInactive() {
            return new ArrayList<>(students.values());
        }
    }
}
