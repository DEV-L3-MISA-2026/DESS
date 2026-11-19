package com.dess.people.application;

import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import java.util.List;
import java.util.Optional;

/** API applicative du module people. */
public interface PeopleService {
    StudentProfile createStudent(CreateStudentCommand command);
    TeacherProfile createTeacher(CreateTeacherCommand command);
    Optional<Person> findById(long id);
    Optional<Person> findByEmail(String email);
    List<StudentProfile> listStudents();
    boolean studentExists(long id);
}
