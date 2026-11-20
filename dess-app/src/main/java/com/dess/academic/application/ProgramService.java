package com.dess.academic.application;

import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.ProgramVersion;
import java.util.List;

/** API applicative des programmes et de leurs versions. */
public interface ProgramService {
    AcademicProgram create(CreateProgramCommand command);
    AcademicProgram activate(long programId);
    AcademicProgram deactivate(long programId);
    List<AcademicProgram> listAll();
    ProgramVersion createVersion(CreateProgramVersionCommand command);
    ProgramVersion archiveVersion(long programVersionId);
    List<ProgramVersion> listVersions(long programId);
}
