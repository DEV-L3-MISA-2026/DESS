package com.dess.academic.application.impl;

import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.CreateProgramVersionCommand;
import com.dess.academic.application.ProgramService;
import com.dess.academic.application.spi.ProgramRepository;
import com.dess.academic.application.spi.ProgramVersionRepository;
import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;
import com.dess.academic.exception.ProgramNotFoundException;
import com.dess.academic.exception.ProgramVersionNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/** Implémentation applicative des programmes et versions. */
@ApplicationScoped
@Transactional
public class ProgramServiceImpl implements ProgramService {
    private final ProgramRepository programRepository;
    private final ProgramVersionRepository versionRepository;

    @Inject
    public ProgramServiceImpl(ProgramRepository programRepository,
                              ProgramVersionRepository versionRepository) {
        this.programRepository = programRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    public AcademicProgram create(CreateProgramCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return programRepository.create(command);
    }

    @Override
    public AcademicProgram activate(long programId) {
        return programRepository.updateStatus(programId, AcademicProgramStatus.ACTIVE);
    }

    @Override
    public AcademicProgram deactivate(long programId) {
        return programRepository.updateStatus(programId, AcademicProgramStatus.INACTIVE);
    }

    @Override
    public List<AcademicProgram> listAll() {
        return programRepository.findAll();
    }

    @Override
    public ProgramVersion createVersion(CreateProgramVersionCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        programRepository.findById(command.programId())
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + command.programId()));
        return versionRepository.create(command);
    }

    @Override
    public ProgramVersion activateVersion(long programVersionId) {
        ProgramVersion version = versionRepository.findById(programVersionId)
                .orElseThrow(() -> new ProgramVersionNotFoundException("Version introuvable : " + programVersionId));
        if (version.status() != ProgramVersionStatus.DRAFT) {
            throw new IllegalStateException("Seule une version DRAFT peut être activée");
        }
        AcademicProgram program = programRepository.findById(version.programId())
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + version.programId()));
        if (program.status() != AcademicProgramStatus.ACTIVE) {
            throw new IllegalStateException("Le programme doit être ACTIVE pour activer une version");
        }
        return versionRepository.updateStatus(programVersionId, ProgramVersionStatus.ACTIVE);
    }

    @Override
    public ProgramVersion archiveVersion(long programVersionId) {
        ProgramVersion version = versionRepository.findById(programVersionId)
                .orElseThrow(() -> new ProgramVersionNotFoundException("Version introuvable : " + programVersionId));
        if (version.status() == ProgramVersionStatus.ARCHIVED) {
            throw new IllegalStateException("La version est déjà archivée");
        }
        return versionRepository.updateStatus(programVersionId, ProgramVersionStatus.ARCHIVED);
    }

    @Override
    public List<ProgramVersion> listVersions(long programId) {
        return versionRepository.findByProgramId(programId);
    }

    @Override
    public Optional<ProgramVersion> findVersion(long programVersionId) {
        return versionRepository.findById(programVersionId);
    }
}
