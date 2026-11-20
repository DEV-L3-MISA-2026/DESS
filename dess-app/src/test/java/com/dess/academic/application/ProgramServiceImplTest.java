package com.dess.academic.application;

import com.dess.academic.application.impl.ProgramServiceImpl;
import com.dess.academic.application.spi.ProgramRepository;
import com.dess.academic.application.spi.ProgramVersionRepository;
import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;
import com.dess.academic.exception.ProgramNotFoundException;
import com.dess.academic.exception.ProgramVersionNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests unitaires des programmes et versions avec ports en mémoire. */
class ProgramServiceImplTest {
    @Test
    void createsVersionAndActivatesIt() {
        InMemoryRepositories repositories = new InMemoryRepositories();
        ProgramService service = repositories.service();
        AcademicProgram program = service.create(new CreateProgramCommand("LICENCE", "Licence Informatique"));
        ProgramVersion draft = service.createVersion(new CreateProgramVersionCommand(
                program.id(), "2026-L3", LocalDate.of(2026, 9, 1), null));

        ProgramVersion active = service.activateVersion(draft.id());
        assertEquals(ProgramVersionStatus.ACTIVE, active.status());
    }

    @Test
    void rejectsActivationWhenProgramIsInactive() {
        InMemoryRepositories repositories = new InMemoryRepositories();
        ProgramService service = repositories.service();
        AcademicProgram program = service.create(new CreateProgramCommand("LICENCE", "Licence Informatique"));
        service.deactivate(program.id());
        ProgramVersion draft = service.createVersion(new CreateProgramVersionCommand(
                program.id(), "2026-L3", LocalDate.of(2026, 9, 1), null));

        assertThrows(IllegalStateException.class, () -> service.activateVersion(draft.id()));
    }

    @Test
    void rejectsActivationWhenVersionIsNotDraft() {
        InMemoryRepositories repositories = new InMemoryRepositories();
        ProgramService service = repositories.service();
        AcademicProgram program = service.create(new CreateProgramCommand("LICENCE", "Licence Informatique"));
        ProgramVersion draft = service.createVersion(new CreateProgramVersionCommand(
                program.id(), "2026-L3", LocalDate.of(2026, 9, 1), null));
        service.archiveVersion(draft.id());

        assertThrows(IllegalStateException.class, () -> service.activateVersion(draft.id()));
    }

    @Test
    void archivesVersionAndFindsIt() {
        InMemoryRepositories repositories = new InMemoryRepositories();
        ProgramService service = repositories.service();
        AcademicProgram program = service.create(new CreateProgramCommand("LICENCE", "Licence Informatique"));
        ProgramVersion draft = service.createVersion(new CreateProgramVersionCommand(
                program.id(), "2026-L3", LocalDate.of(2026, 9, 1), null));

        assertEquals(ProgramVersionStatus.ARCHIVED, service.archiveVersion(draft.id()).status());
        assertEquals(ProgramVersionStatus.ARCHIVED,
                service.findVersion(draft.id()).orElseThrow().status());
    }

    @Test
    void rejectsInvalidDatesAndUnknownObjects() {
        assertThrows(IllegalArgumentException.class, () -> new CreateProgramVersionCommand(
                1L, "2026-L3", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)));

        InMemoryRepositories repositories = new InMemoryRepositories();
        ProgramService service = repositories.service();
        assertThrows(ProgramNotFoundException.class, () -> service.createVersion(new CreateProgramVersionCommand(
                999L, "2026-L3", LocalDate.of(2026, 9, 1), null)));
        assertThrows(ProgramVersionNotFoundException.class, () -> service.activateVersion(999L));
    }

    private static class InMemoryRepositories {
        private final InMemoryProgramRepository programs = new InMemoryProgramRepository();
        private final InMemoryVersionRepository versions = new InMemoryVersionRepository();

        ProgramService service() {
            return new ProgramServiceImpl(programs, versions);
        }

        private class InMemoryProgramRepository implements ProgramRepository {
            private final Map<Long, AcademicProgram> values = new HashMap<>();
            private long nextId = 1L;

            @Override
            public AcademicProgram create(CreateProgramCommand command) {
                AcademicProgram value = new AcademicProgram(nextId++, command.code().trim(),
                        command.name().trim(), AcademicProgramStatus.ACTIVE);
                values.put(value.id(), value);
                return value;
            }

            @Override
            public Optional<AcademicProgram> findById(long id) {
                return Optional.ofNullable(values.get(id));
            }

            @Override
            public AcademicProgram updateStatus(long id, AcademicProgramStatus status) {
                AcademicProgram current = values.get(id);
                if (current == null) throw new ProgramNotFoundException("Programme introuvable : " + id);
                AcademicProgram updated = new AcademicProgram(id, current.code(), current.name(), status);
                values.put(id, updated);
                return updated;
            }

            @Override
            public List<AcademicProgram> findAll() {
                return new ArrayList<>(values.values());
            }
        }

        private class InMemoryVersionRepository implements ProgramVersionRepository {
            private final Map<Long, ProgramVersion> values = new HashMap<>();
            private long nextId = 1L;

            @Override
            public ProgramVersion create(CreateProgramVersionCommand command) {
                ProgramVersion value = new ProgramVersion(nextId++, command.programId(), command.code().trim(),
                        command.effectiveFrom(), command.effectiveTo(), ProgramVersionStatus.DRAFT);
                values.put(value.id(), value);
                return value;
            }

            @Override
            public Optional<ProgramVersion> findById(long id) {
                return Optional.ofNullable(values.get(id));
            }

            @Override
            public ProgramVersion updateStatus(long id, ProgramVersionStatus status) {
                ProgramVersion current = values.get(id);
                if (current == null) throw new ProgramVersionNotFoundException("Version introuvable : " + id);
                ProgramVersion updated = new ProgramVersion(id, current.programId(), current.code(),
                        current.effectiveFrom(), current.effectiveTo(), status);
                values.put(id, updated);
                return updated;
            }

            @Override
            public List<ProgramVersion> findByProgramId(long programId) {
                return values.values().stream().filter(value -> value.programId() == programId).toList();
            }
        }
    }
}
