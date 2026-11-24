package com.dess.academic.application;

import com.dess.people.application.PeopleService;
import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;

import java.util.List;
import java.util.Optional;

/** Fake minimal de PeopleService pour les tests d'inscription. */
public class InMemoryPeopleService implements PeopleService {
    private final boolean studentExists;

    public InMemoryPeopleService(boolean studentExists) {
        this.studentExists = studentExists;
    }

    @Override public StudentProfile createStudent(CreateStudentCommand command) { throw new UnsupportedOperationException(); }
    @Override public TeacherProfile createTeacher(CreateTeacherCommand command) { throw new UnsupportedOperationException(); }
    @Override public Optional<Person> findById(long id) { return Optional.empty(); }
    @Override public Optional<Person> findByEmail(String email) { return Optional.empty(); }
    @Override public List<StudentProfile> listStudents() { return List.of(); }
    @Override public boolean studentExists(long id) { return studentExists && id == 1L; }
}
