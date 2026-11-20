package com.dess.people.application.spi;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.domain.StudentProfile;
import java.util.List;

/** Port de persistance pour l’agrégat étudiant. */
public interface StudentRepository {
    StudentProfile create(CreateStudentCommand command);

    List<StudentProfile> findAll();

    boolean exists(long id);
}
