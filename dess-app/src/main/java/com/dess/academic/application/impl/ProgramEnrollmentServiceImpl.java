package com.dess.academic.application.impl;

import com.dess.academic.application.EnrollStudentCommand;
import com.dess.academic.application.ProgramEnrollmentService;
import com.dess.academic.application.ProgramService;
import com.dess.academic.application.spi.ProgramEnrollmentRepository;
import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;
import com.dess.academic.exception.EnrollmentConflictException;
import com.dess.academic.exception.InvalidEnrollmentException;
import com.dess.academic.exception.ProgramNotFoundException;
import com.dess.academic.exception.ProgramVersionNotFoundException;
import com.dess.people.application.PeopleService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Implémentation des règles d'inscription aux versions de programmes. */
@ApplicationScoped
@Transactional
public class ProgramEnrollmentServiceImpl implements ProgramEnrollmentService {
    private final ProgramEnrollmentRepository repository;
    private final ProgramService programService;
    private final PeopleService peopleService;

    @Inject
    public ProgramEnrollmentServiceImpl(ProgramEnrollmentRepository repository,
                                       ProgramService programService,
                                       PeopleService peopleService) {
        this.repository = repository;
        this.programService = programService;
        this.peopleService = peopleService;
    }

    @Override
    public ProgramEnrollment enroll(EnrollStudentCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        if (!peopleService.studentExists(command.studentId())) {
            throw new InvalidEnrollmentException("Étudiant introuvable : " + command.studentId());
        }

        ProgramVersion version = programService.findVersion(command.programVersionId())
                .orElseThrow(() -> new ProgramVersionNotFoundException(
                        "Version introuvable : " + command.programVersionId()));
        AcademicProgram program = findProgram(version.programId());

        if (program.status() != AcademicProgramStatus.ACTIVE) {
            throw new InvalidEnrollmentException("Le programme n'est pas actif");
        }
        if (version.status() != ProgramVersionStatus.ACTIVE) {
            throw new InvalidEnrollmentException("La version n'est pas active");
        }

        validateEffectDates(command.startDate(), command.endDate(), version);
        if (repository.hasActiveEnrollment(command.studentId(), command.programVersionId())) {
            throw new EnrollmentConflictException(
                    "Une inscription ACTIVE existe déjà pour cet étudiant et cette version");
        }

        ProgramEnrollment enrollment = new ProgramEnrollment(null, command.studentId(),
                command.programVersionId(), command.startDate(), command.endDate(), EnrollmentStatus.ACTIVE);
        return repository.create(enrollment);
    }

    @Override
    public ProgramEnrollment complete(long enrollmentId, LocalDate endDate) {
        return updateStatus(enrollmentId, endDate, EnrollmentStatus.COMPLETED);
    }

    @Override
    public ProgramEnrollment withdraw(long enrollmentId, LocalDate endDate) {
        return updateStatus(enrollmentId, endDate, EnrollmentStatus.WITHDRAWN);
    }

    private ProgramEnrollment updateStatus(long enrollmentId, LocalDate endDate,
                                           EnrollmentStatus targetStatus) {
        ProgramEnrollment enrollment = repository.findById(enrollmentId)
                .orElseThrow(() -> new InvalidEnrollmentException("Inscription introuvable : " + enrollmentId));
        if (enrollment.status() != EnrollmentStatus.ACTIVE) {
            throw new InvalidEnrollmentException("Seule une inscription ACTIVE peut être terminée ou retirée");
        }
        if (endDate != null && endDate.isBefore(enrollment.startDate())) {
            throw new InvalidEnrollmentException("La fin d'inscription ne peut précéder son début");
        }

        ProgramEnrollment updated = new ProgramEnrollment(enrollment.id(), enrollment.studentId(),
                enrollment.programVersionId(), enrollment.startDate(), endDate, targetStatus);
        return repository.update(updated);
    }

    @Override
    public List<ProgramEnrollment> listByStudent(long studentId) {
        if (studentId <= 0) throw new IllegalArgumentException("L'identifiant étudiant doit être positif");
        return repository.findByStudentId(studentId);
    }

    private AcademicProgram findProgram(long programId) {
        return programService.listAll().stream()
                .filter(program -> program.id() != null && program.id() == programId)
                .findFirst()
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + programId));
    }

    private void validateEffectDates(LocalDate startDate, LocalDate endDate, ProgramVersion version) {
        if (startDate == null) throw new InvalidEnrollmentException("La date de début est obligatoire");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidEnrollmentException("La fin d'inscription ne peut précéder son début");
        }
        if (startDate.isBefore(version.effectiveFrom())
                || (version.effectiveTo() != null && startDate.isAfter(version.effectiveTo()))) {
            throw new InvalidEnrollmentException("La date de début est hors de la période d'effet de la version");
        }
        if (endDate != null
                && (endDate.isBefore(version.effectiveFrom())
                    || (version.effectiveTo() != null && endDate.isAfter(version.effectiveTo())))) {
            throw new InvalidEnrollmentException("La date de fin est hors de la période d'effet de la version");
        }
    }
}
