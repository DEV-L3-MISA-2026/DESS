package com.dess.people.application;

import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.domain.TeacherProfile;
import java.util.List;
import java.util.Optional;

/** API applicative du module people. */
public interface PeopleService {
    StudentProfile createStudent(CreateStudentCommand command);

    default Person updateStudent(long id, UpdateStudentCommand command) {
        throw new UnsupportedOperationException("Student updates are not implemented by this service");
    }

    default void deactivateStudent(long id) {
        throw new UnsupportedOperationException("Student deactivation is not implemented by this service");
    }

    TeacherProfile createTeacher(CreateTeacherCommand command);

    Optional<Person> findById(long id);

    Optional<Person> findByEmail(String email);

    List<StudentProfile> listStudents();

    default List<StudentProfile> listStudentsIncludingInactive() {
        return listStudents();
    }

    boolean studentExists(long id);
}
