package com.dess.academic.infrastructure;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.CreateProgramVersionCommand;
import com.dess.academic.application.EnrollStudentCommand;
import com.dess.academic.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port d'accès aux données du module academic. */
public interface AcademicRepository {
    AcademicPeriod createPeriod(CreatePeriodCommand command);
    List<AcademicPeriod> listPeriods();
    AcademicProgram createProgram(CreateProgramCommand command);
    Optional<AcademicProgram> findProgram(long programId);
    AcademicProgram saveProgram(AcademicProgram program);
    List<AcademicProgram> listPrograms();
    ProgramVersion createVersion(CreateProgramVersionCommand command);
    Optional<ProgramVersion> findVersion(long programVersionId);
    ProgramVersion saveVersion(ProgramVersion version);
    List<ProgramVersion> listVersions(long programId);
    ProgramEnrollment createEnrollment(EnrollStudentCommand command);
    Optional<ProgramEnrollment> findEnrollment(long enrollmentId);
    ProgramEnrollment saveEnrollment(ProgramEnrollment enrollment);
    List<ProgramEnrollment> listEnrollmentsByStudent(long studentId);
    boolean hasActiveEnrollment(long studentId, long programVersionId);
}
