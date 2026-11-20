package com.dess.academic.application.impl;

import com.dess.academic.application.*;
import com.dess.academic.domain.*;
import com.dess.academic.exception.ProgramNotFoundException;
import com.dess.academic.exception.ProgramVersionNotFoundException;
import com.dess.academic.infrastructure.AcademicRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/** Implémentation applicative des programmes et versions. */
@ApplicationScoped
@Transactional
public class ProgramServiceImpl implements ProgramService {
    private final AcademicRepository repository;

    @Inject
    public ProgramServiceImpl(AcademicRepository repository) { this.repository = repository; }

    @Override
    public AcademicProgram create(CreateProgramCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return repository.createProgram(command);
    }

    @Override
    public AcademicProgram activate(long programId) {
        AcademicProgram program = repository.findProgram(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + programId));
        return repository.saveProgram(new AcademicProgram(program.id(), program.code(), program.name(), AcademicProgramStatus.ACTIVE));
    }

    @Override
    public AcademicProgram deactivate(long programId) {
        AcademicProgram program = repository.findProgram(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + programId));
        return repository.saveProgram(new AcademicProgram(program.id(), program.code(), program.name(), AcademicProgramStatus.INACTIVE));
    }

    @Override
    public List<AcademicProgram> listAll() { return repository.listPrograms(); }

    @Override
    public ProgramVersion createVersion(CreateProgramVersionCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        repository.findProgram(command.programId())
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + command.programId()));
        return repository.createVersion(command);
    }

    @Override
    public ProgramVersion archiveVersion(long programVersionId) {
        ProgramVersion version = repository.findVersion(programVersionId)
                .orElseThrow(() -> new ProgramVersionNotFoundException("Version introuvable : " + programVersionId));
        return repository.saveVersion(new ProgramVersion(version.id(), version.programId(), version.code(),
                version.effectiveFrom(), version.effectiveTo(), ProgramVersionStatus.ARCHIVED));
    }

    @Override
    public List<ProgramVersion> listVersions(long programId) { return repository.listVersions(programId); }
}
