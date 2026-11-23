package com.dess.academic.application;

import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Fake minimal de ProgramService pour les tests d'inscription. */
public class InMemoryProgramService implements ProgramService {
    private AcademicProgram program = new AcademicProgram(10L, "LICENCE", "Licence",
            AcademicProgramStatus.ACTIVE);
    private ProgramVersion version = new ProgramVersion(20L, 10L, "2026",
            LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30),
            ProgramVersionStatus.ACTIVE);

    public void setProgram(AcademicProgram program) {
        this.program = program;
    }

    public void setVersion(ProgramVersion version) {
        this.version = version;
    }

    @Override public AcademicProgram create(CreateProgramCommand command) { throw new UnsupportedOperationException(); }
    @Override public AcademicProgram activate(long programId) { throw new UnsupportedOperationException(); }
    @Override public AcademicProgram deactivate(long programId) { throw new UnsupportedOperationException(); }
    @Override public List<AcademicProgram> listAll() { return List.of(program); }
    @Override public ProgramVersion createVersion(CreateProgramVersionCommand command) { throw new UnsupportedOperationException(); }
    @Override public ProgramVersion activateVersion(long programVersionId) { throw new UnsupportedOperationException(); }
    @Override public ProgramVersion archiveVersion(long programVersionId) { throw new UnsupportedOperationException(); }
    @Override public List<ProgramVersion> listVersions(long programId) { return List.of(version); }
    @Override public Optional<ProgramVersion> findVersion(long programVersionId) {
        return programVersionId == version.id() ? Optional.of(version) : Optional.empty();
    }
}
