package com.dess.people.application.impl;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.application.spi.PersonRepository;
import com.dess.people.application.spi.StudentRepository;
import com.dess.people.application.spi.TeacherRepository;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/** Implémentation applicative du module people. */
@ApplicationScoped
@Transactional
public class PeopleServiceImpl implements PeopleService {
    private final PersonRepository personRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    @Inject
    public PeopleServiceImpl(PersonRepository personRepository,
                             StudentRepository studentRepository,
                             TeacherRepository teacherRepository) {
        this.personRepository = personRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    @Override
    public StudentProfile createStudent(CreateStudentCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return studentRepository.create(command);
    }

    @Override
    public TeacherProfile createTeacher(CreateTeacherCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return teacherRepository.create(command);
    }

    @Override
    public Optional<Person> findById(long id) {
        return personRepository.findById(id);
    }

    @Override
    public Optional<Person> findByEmail(String email) {
        if (email == null || email.trim().isBlank()) throw new IllegalArgumentException("L’adresse e-mail est obligatoire");
        return personRepository.findByEmail(email.trim());
    }

    @Override
    public List<StudentProfile> listStudents() {
        return studentRepository.findAll();
    }

    @Override
    public boolean studentExists(long id) {
        return studentRepository.exists(id);
    }
}
