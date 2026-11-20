package com.dess.academic.infrastructure;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.CreateProgramVersionCommand;
import com.dess.academic.application.EnrollStudentCommand;
import com.dess.academic.domain.*;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.DuplicatePeriodException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;

/** Repository JPA du module academic conservé pour les inscriptions. */
@ApplicationScoped
public class JpaAcademicRepository implements AcademicRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaAcademicRepository() {}

    public JpaAcademicRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public AcademicPeriod createPeriod(CreatePeriodCommand command) {
        try {
            PeriodEntity entity = new PeriodEntity(command.academicYear(), command.term());
            entityManager.persist(entity);
            entityManager.flush();
            return new AcademicPeriod(entity.getId(), entity.getAcademicYear(), entity.getTerm());
        } catch (PersistenceException e) {
            throw new DuplicatePeriodException("Cette période académique existe déjà", e);
        }
    }

    @Override
    public List<AcademicPeriod> listPeriods() {
        return entityManager.createQuery("select p from PeriodEntity p", PeriodEntity.class).getResultList().stream()
                .map(p -> new AcademicPeriod(p.getId(), p.getAcademicYear(), p.getTerm())).toList();
    }

    @Override
    public AcademicProgram createProgram(CreateProgramCommand command) {
        try {
            AcademicProgramEntity entity = new AcademicProgramEntity(command.code().trim(), command.name().trim(), "ACTIVE");
            entityManager.persist(entity);
            entityManager.flush();
            return toProgram(entity);
        } catch (PersistenceException e) {
            throw new AcademicException("Impossible de créer le programme : code déjà utilisé ou contrainte invalide", e);
        }
    }

    @Override
    public Optional<AcademicProgram> findProgram(long programId) {
        return Optional.ofNullable(entityManager.find(AcademicProgramEntity.class, programId)).map(this::toProgram);
    }

    @Override
    public AcademicProgram saveProgram(AcademicProgram program) {
        AcademicProgramEntity entity = entityManager.find(AcademicProgramEntity.class, program.id());
        if (entity == null) throw new AcademicException("Programme introuvable : " + program.id());
        entity.setStatus(program.status().name());
        entityManager.flush();
        return toProgram(entity);
    }

    @Override
    public List<AcademicProgram> listPrograms() {
        return entityManager.createQuery("select p from AcademicProgramEntity p", AcademicProgramEntity.class).getResultList().stream()
                .map(this::toProgram).toList();
    }

    @Override
    public ProgramVersion createVersion(CreateProgramVersionCommand command) {
        try {
            ProgramVersionEntity entity = new ProgramVersionEntity(command.programId(), command.code().trim(),
                    command.effectiveFrom(), command.effectiveTo(), "DRAFT");
            new ProgramVersion(null, command.programId(), command.code(), command.effectiveFrom(), command.effectiveTo(), ProgramVersionStatus.DRAFT);
            entityManager.persist(entity);
            entityManager.flush();
            return toVersion(entity);
        } catch (PersistenceException e) {
            throw new AcademicException("Impossible de créer la version : code dupliqué ou dates invalides", e);
        }
    }

    @Override
    public Optional<ProgramVersion> findVersion(long programVersionId) {
        return Optional.ofNullable(entityManager.find(ProgramVersionEntity.class, programVersionId)).map(this::toVersion);
    }

    @Override
    public ProgramVersion saveVersion(ProgramVersion version) {
        ProgramVersionEntity entity = entityManager.find(ProgramVersionEntity.class, version.id());
        if (entity == null) throw new AcademicException("Version introuvable : " + version.id());
        entity.setStatus(version.status().name());
        entityManager.flush();
        return toVersion(entity);
    }

    @Override
    public List<ProgramVersion> listVersions(long programId) {
        return entityManager.createQuery("select v from ProgramVersionEntity v where v.programId = :programId", ProgramVersionEntity.class)
                .setParameter("programId", programId).getResultList().stream().map(this::toVersion).toList();
    }

    @Override
    public ProgramEnrollment createEnrollment(EnrollStudentCommand command) {
        try {
            ProgramEnrollmentEntity entity = new ProgramEnrollmentEntity(command.studentId(), command.programVersionId(),
                    command.startDate(), command.endDate(), "ACTIVE");
            entityManager.persist(entity);
            entityManager.flush();
            return toEnrollment(entity);
        } catch (PersistenceException e) {
            throw new AcademicException("Impossible de créer l'inscription : contrainte de base de données violée", e);
        }
    }

    @Override
    public Optional<ProgramEnrollment> findEnrollment(long enrollmentId) {
        return Optional.ofNullable(entityManager.find(ProgramEnrollmentEntity.class, enrollmentId)).map(this::toEnrollment);
    }

    @Override
    public ProgramEnrollment saveEnrollment(ProgramEnrollment enrollment) {
        ProgramEnrollmentEntity entity = entityManager.find(ProgramEnrollmentEntity.class, enrollment.id());
        if (entity == null) throw new AcademicException("Inscription introuvable : " + enrollment.id());
        entity.setEndDate(enrollment.endDate());
        entity.setStatus(enrollment.status().name());
        entityManager.flush();
        return toEnrollment(entity);
    }

    @Override
    public List<ProgramEnrollment> listEnrollmentsByStudent(long studentId) {
        return entityManager.createQuery("select e from ProgramEnrollmentEntity e where e.studentId = :studentId", ProgramEnrollmentEntity.class)
                .setParameter("studentId", studentId).getResultList().stream().map(this::toEnrollment).toList();
    }

    @Override
    public boolean hasActiveEnrollment(long studentId, long programVersionId) {
        Long count = entityManager.createQuery("select count(e) from ProgramEnrollmentEntity e "
                        + "where e.studentId = :studentId and e.programVersionId = :programVersionId and e.status = 'ACTIVE'", Long.class)
                .setParameter("studentId", studentId).setParameter("programVersionId", programVersionId).getSingleResult();
        return count != null && count > 0;
    }

    private AcademicProgram toProgram(AcademicProgramEntity entity) {
        return new AcademicProgram(entity.getId(), entity.getCode(), entity.getName(),
                AcademicProgramStatus.valueOf(entity.getStatus()));
    }

    private ProgramVersion toVersion(ProgramVersionEntity entity) {
        return new ProgramVersion(entity.getId(), entity.getProgramId(), entity.getCode(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), ProgramVersionStatus.valueOf(entity.getStatus()));
    }

    private ProgramEnrollment toEnrollment(ProgramEnrollmentEntity entity) {
        return new ProgramEnrollment(entity.getId(), entity.getStudentId(), entity.getProgramVersionId(),
                entity.getStartDate(), entity.getEndDate(), EnrollmentStatus.valueOf(entity.getStatus()));
    }
}
