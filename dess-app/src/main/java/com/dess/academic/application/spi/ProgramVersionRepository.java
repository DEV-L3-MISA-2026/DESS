package com.dess.academic.application.spi;

import com.dess.academic.application.CreateProgramVersionCommand;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;
import java.util.List;
import java.util.Optional;

/** Port de persistance pour les versions de programme. */
public interface ProgramVersionRepository {
    ProgramVersion create(CreateProgramVersionCommand command);
    Optional<ProgramVersion> findById(long id);
    ProgramVersion updateStatus(long id, ProgramVersionStatus status);
    List<ProgramVersion> findByProgramId(long programId);
}
