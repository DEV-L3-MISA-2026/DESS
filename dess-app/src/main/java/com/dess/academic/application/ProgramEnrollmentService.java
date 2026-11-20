package com.dess.academic.application;

import com.dess.academic.domain.ProgramEnrollment;
import java.time.LocalDate;
import java.util.List;

/** API applicative des inscriptions aux versions de programmes. */
public interface ProgramEnrollmentService {
    ProgramEnrollment enroll(EnrollStudentCommand command);
    ProgramEnrollment complete(long enrollmentId, LocalDate endDate);
    ProgramEnrollment withdraw(long enrollmentId, LocalDate endDate);
    List<ProgramEnrollment> listByStudent(long studentId);
}
