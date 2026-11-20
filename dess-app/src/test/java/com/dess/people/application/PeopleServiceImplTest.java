package com.dess.people.application;

import com.dess.people.application.impl.PeopleServiceImpl;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import com.dess.people.infrastructure.PeopleRepository;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires du service people avec repository en mémoire. */
class PeopleServiceImplTest {
    @Test
    void createsAndFindsStudent() {
        InMemoryPeopleRepository repository = new InMemoryPeopleRepository();
        PeopleService service = new PeopleServiceImpl(repository);
        StudentProfile student = service.createStudent(new CreateStudentCommand("student@dess.mg", "Rakoto", "Jean", "ETU001"));

        assertTrue(service.studentExists(student.personId()));
        assertEquals("student@dess.mg", service.findByEmail("student@dess.mg").orElseThrow().email());
        assertEquals(List.of(student), service.listStudents());
    }

    @Test
    void createsTeacher() {
        PeopleService service = new PeopleServiceImpl(new InMemoryPeopleRepository());
        TeacherProfile teacher = service.createTeacher(new CreateTeacherCommand("teacher@dess.mg", "Rabe", "Paul", "Professeur"));
        assertEquals("Professeur", teacher.academicRank());
    }

    private static class InMemoryPeopleRepository implements PeopleRepository {
        private final Map<Long, Person> persons = new HashMap<>();
        private final Map<Long, StudentProfile> students = new HashMap<>();
        private final Map<Long, TeacherProfile> teachers = new HashMap<>();
        private long nextId = 1L;

        @Override
        public StudentProfile createStudent(CreateStudentCommand command) {
            long id = nextId++;
            persons.put(id, new Person(id, command.email(), command.lastName(), command.firstName(), true));
            StudentProfile profile = new StudentProfile(id, command.registrationNumber());
            students.put(id, profile);
            return profile;
        }

        @Override
        public TeacherProfile createTeacher(CreateTeacherCommand command) {
            long id = nextId++;
            persons.put(id, new Person(id, command.email(), command.lastName(), command.firstName(), true));
            TeacherProfile profile = new TeacherProfile(id, command.academicRank());
            teachers.put(id, profile);
            return profile;
        }

        @Override public Optional<Person> findById(long id) { return Optional.ofNullable(persons.get(id)); }
        @Override public Optional<Person> findByEmail(String email) { return persons.values().stream().filter(p -> p.email().equals(email)).findFirst(); }
        @Override public List<StudentProfile> listStudents() { return List.copyOf(students.values()); }
        @Override public boolean studentExists(long id) { return students.containsKey(id); }
    }
}
