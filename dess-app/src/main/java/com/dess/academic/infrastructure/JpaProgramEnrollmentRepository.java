package com.dess.academic.infrastructure;

import com.dess.academic.application.spi.ProgramEnrollmentRepository;
import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.EnrollmentConflictException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;

/** Implémentation JPA du port des inscriptions aux programmes. */
@ApplicationScoped
public class JpaProgramEnrollmentRepository implements ProgramEnrollmentRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaProgramEnrollmentRepository() {}

    public JpaProgramEnrollmentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ProgramEnrollment create(ProgramEnrollment enrollment) {
        ProgramEnrollment domain = new ProgramEnrollment(enrollment.id(), enrollment.studentId(),
                enrollment.programVersionId(), enrollment.startDate(), enrollment.endDate(), enrollment.status());
        try {
            ProgramEnrollmentEntity entity = new ProgramEnrollmentEntity(domain.studentId(),
                    domain.programVersionId(), domain.startDate(), domain.endDate(), domain.status().name());
            entityManager.persist(entity);
            entityManager.flush();
            return toDomain(entity);
        } catch (PersistenceException exception) {
            throw translateConflict(exception);
        }
    }

    @Override
    public Optional<ProgramEnrollment> findById(long id) {
        return Optional.ofNullable(entityManager.find(ProgramEnrollmentEntity.class, id)).map(this::toDomain);
    }

    @Override
    public ProgramEnrollment update(ProgramEnrollment enrollment) {
        ProgramEnrollment domain = new ProgramEnrollment(enrollment.id(), enrollment.studentId(),
                enrollment.programVersionId(), enrollment.startDate(), enrollment.endDate(), enrollment.status());
        ProgramEnrollmentEntity entity = entityManager.find(ProgramEnrollmentEntity.class, domain.id());
        if (entity == null) throw new AcademicException("Inscription introuvable : " + domain.id());
        entity.setEndDate(domain.endDate());
        entity.setStatus(domain.status().name());
        try {
            entityManager.flush();
            return toDomain(entity);
        } catch (PersistenceException exception) {
            throw new AcademicException("Impossible de mettre à jour l'inscription", exception);
        }
    }

    @Override
    public List<ProgramEnrollment> findByStudentId(long studentId) {
        return entityManager.createQuery(
                        "select e from ProgramEnrollmentEntity e where e.studentId = :studentId",
                        ProgramEnrollmentEntity.class)
                .setParameter("studentId", studentId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean hasActiveEnrollment(long studentId, long programVersionId) {
        Long count = entityManager.createQuery(
                        "select count(e) from ProgramEnrollmentEntity e "
                                + "where e.studentId = :studentId and e.programVersionId = :programVersionId "
                                + "and e.status = 'ACTIVE'", Long.class)
                .setParameter("studentId", studentId)
                .setParameter("programVersionId", programVersionId)
                .getSingleResult();
        return count != null && count > 0;
    }

    private EnrollmentConflictException translateConflict(PersistenceException exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String detail = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
        if (detail.contains("uq_program_enrollment_active_student_version")
                || detail.contains("23505")) {
            return new EnrollmentConflictException(
                    "Une inscription ACTIVE existe déjà pour cet étudiant et cette version",
                    exception);
        }
        throw new AcademicException("Impossible de créer l'inscription : contrainte de base de données violée",
                exception);
    }

    private ProgramEnrollment toDomain(ProgramEnrollmentEntity entity) {
        return new ProgramEnrollment(entity.getId(), entity.getStudentId(), entity.getProgramVersionId(),
                entity.getStartDate(), entity.getEndDate(), EnrollmentStatus.valueOf(entity.getStatus()));
    }
}
