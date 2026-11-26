package com.dess.people.application.spi;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.UpdateStudentCommand;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import java.util.List;

/** Port de persistance pour l’agrégat étudiant. */
public interface StudentRepository {
    StudentProfile create(CreateStudentCommand command);

    Person update(long id, UpdateStudentCommand command);

    void deactivate(long id);

    List<StudentProfile> findAll();

    List<StudentProfile> findAllIncludingInactive();

    boolean exists(long id);
}
