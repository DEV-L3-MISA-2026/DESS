package com.dess.people.infrastructure;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import java.util.List;
import java.util.Optional;

/** Port d'accès aux données people utilisé par la couche applicative. */
public interface PeopleRepository {
    StudentProfile createStudent(CreateStudentCommand command);
    TeacherProfile createTeacher(CreateTeacherCommand command);
    Optional<Person> findById(long id);
    Optional<Person> findByEmail(String email);
    List<StudentProfile> listStudents();
    boolean studentExists(long id);
}
