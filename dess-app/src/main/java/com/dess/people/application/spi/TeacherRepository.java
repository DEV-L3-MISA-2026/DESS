package com.dess.people.application.spi;

import com.dess.people.application.CreateTeacherCommand;
import com.dess.people.domain.TeacherProfile;

/** Port de persistance pour l’agrégat enseignant. */
public interface TeacherRepository {
    TeacherProfile create(CreateTeacherCommand command);
}
