package com.dess.people.application.impl;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import com.dess.people.infrastructure.PeopleRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/** Implémentation applicative du module people. */
@Transactional
public class PeopleServiceImpl implements PeopleService {
    private final PeopleRepository repository;

    public PeopleServiceImpl(PeopleRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudentProfile createStudent(CreateStudentCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return repository.createStudent(command);
    }

    @Override
    public TeacherProfile createTeacher(CreateTeacherCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return repository.createTeacher(command);
    }

    @Override
    public Optional<Person> findById(long id) { return repository.findById(id); }

    @Override
    public Optional<Person> findByEmail(String email) { return repository.findByEmail(email); }

    @Override
    public List<StudentProfile> listStudents() { return repository.listStudents(); }

    @Override
    public boolean studentExists(long id) { return repository.studentExists(id); }
}
