package com.dess.academic.application.spi;

import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import java.util.List;
import java.util.Optional;

/** Port de persistance pour les programmes académiques. */
public interface ProgramRepository {
    AcademicProgram create(CreateProgramCommand command);
    Optional<AcademicProgram> findById(long id);
    AcademicProgram updateStatus(long id, AcademicProgramStatus status);
    List<AcademicProgram> findAll();
}
