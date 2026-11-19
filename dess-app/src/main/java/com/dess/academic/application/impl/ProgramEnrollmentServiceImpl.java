package com.dess.academic.application.impl;

import com.dess.people.application.PeopleService;
import com.dess.academic.application.EnrollStudentCommand;
import com.dess.academic.application.ProgramEnrollmentService;
import com.dess.academic.domain.*;
import com.dess.academic.exception.*;
import com.dess.academic.infrastructure.AcademicRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Implémentation des règles d'inscription aux versions de programmes. */
@Transactional
public class ProgramEnrollmentServiceImpl implements ProgramEnrollmentService {
    private final AcademicRepository academicRepository;
    private final PeopleService peopleService;

    public ProgramEnrollmentServiceImpl(AcademicRepository academicRepository, PeopleService peopleService) {
        this.academicRepository = academicRepository;
        this.peopleService = peopleService;
    }

    @Override
    public ProgramEnrollment enroll(EnrollStudentCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        if (!peopleService.studentExists(command.studentId())) {
            throw new InvalidEnrollmentException("Étudiant introuvable : " + command.studentId());
        }
        ProgramVersion version = academicRepository.findVersion(command.programVersionId())
                .orElseThrow(() -> new ProgramVersionNotFoundException("Version introuvable : " + command.programVersionId()));
        AcademicProgram program = academicRepository.findProgram(version.programId())
                .orElseThrow(() -> new ProgramNotFoundException("Programme introuvable : " + version.programId()));
        if (program.status() != AcademicProgramStatus.ACTIVE) {
            throw new InvalidEnrollmentException("Le programme n'est pas actif");
        }
        if (version.status() != ProgramVersionStatus.ACTIVE) {
            throw new InvalidEnrollmentException("La version n'est pas active");
        }
        if (command.endDate() != null && command.endDate().isBefore(command.startDate())) {
            throw new InvalidEnrollmentException("La fin d'inscription ne peut précéder son début");
        }
        if (command.startDate().isBefore(version.effectiveFrom())
                || (version.effectiveTo() != null && command.startDate().isAfter(version.effectiveTo()))) {
            throw new InvalidEnrollmentException("La date de début est hors de la période d'effet de la version");
        }
        if (command.endDate() != null
                && (command.endDate().isBefore(version.effectiveFrom())
                    || (version.effectiveTo() != null && command.endDate().isAfter(version.effectiveTo())))) {
            throw new InvalidEnrollmentException("La date de fin est hors de la période d'effet de la version");
        }
        if (academicRepository.hasActiveEnrollment(command.studentId(), command.programVersionId())) {
            throw new EnrollmentConflictException("Une inscription ACTIVE existe déjà pour cet étudiant et cette version");
        }
        return academicRepository.createEnrollment(command);
    }

    @Override
    public ProgramEnrollment complete(long enrollmentId, LocalDate endDate) {
        return updateStatus(enrollmentId, endDate, EnrollmentStatus.COMPLETED);
    }

    @Override
    public ProgramEnrollment withdraw(long enrollmentId, LocalDate endDate) {
        return updateStatus(enrollmentId, endDate, EnrollmentStatus.WITHDRAWN);
    }

    private ProgramEnrollment updateStatus(long enrollmentId, LocalDate endDate, EnrollmentStatus status) {
        ProgramEnrollment enrollment = academicRepository.findEnrollment(enrollmentId)
                .orElseThrow(() -> new InvalidEnrollmentException("Inscription introuvable : " + enrollmentId));
        if (endDate != null && endDate.isBefore(enrollment.startDate())) {
            throw new InvalidEnrollmentException("La fin d'inscription ne peut précéder son début");
        }
        return academicRepository.saveEnrollment(new ProgramEnrollment(enrollment.id(), enrollment.studentId(),
                enrollment.programVersionId(), enrollment.startDate(), endDate, status));
    }

    @Override
    public List<ProgramEnrollment> listByStudent(long studentId) {
        return academicRepository.listEnrollmentsByStudent(studentId);
    }
}
