package com.dess.academic.application.spi;

import com.dess.academic.domain.ProgramEnrollment;
import java.util.List;
import java.util.Optional;

/** Port de persistance pour les inscriptions aux versions de programmes. */
public interface ProgramEnrollmentRepository {
    ProgramEnrollment create(ProgramEnrollment enrollment);
    Optional<ProgramEnrollment> findById(long id);
    ProgramEnrollment update(ProgramEnrollment enrollment);
    List<ProgramEnrollment> findByStudentId(long studentId);
    boolean hasActiveEnrollment(long studentId, long programVersionId);
}
