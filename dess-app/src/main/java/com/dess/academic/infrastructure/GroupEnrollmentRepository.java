package com.dess.academic.infrastructure;

import com.dess.academic.domain.GroupEnrollment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository JPA des inscriptions de groupes. */
@ApplicationScoped
public class GroupEnrollmentRepository
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public GroupEnrollmentRepository() {}

    public GroupEnrollmentRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    public GroupEnrollment save(GroupEnrollment enrollment)
    {
        return toDomain(entityManager.merge(toEntity(enrollment)));
    }

    public Optional<GroupEnrollment> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(GroupEnrollmentEntity.class, id))
                .map(this::toDomain);
    }

    public boolean exists(UUID studentId, UUID groupId, UUID periodId)
    {
        return entityManager.createQuery(
                        "select count(e) from GroupEnrollmentEntity e "
                                + "where e.studentId = :studentId and e.groupId = :groupId "
                                + "and e.academicPeriodId = :periodId",
                        Long.class)
                .setParameter("studentId", studentId)
                .setParameter("groupId", groupId)
                .setParameter("periodId", periodId)
                .getSingleResult() > 0;
    }

    public void delete(UUID enrollmentId)
    {
        GroupEnrollmentEntity entity = entityManager.find(GroupEnrollmentEntity.class, enrollmentId);
        if (entity != null) entityManager.remove(entity);
    }

    public List<GroupEnrollment> findByGroupAndPeriod(UUID groupId, UUID periodId)
    {
        var query = entityManager.createQuery(
                "select e from GroupEnrollmentEntity e "
                        + "where e.groupId = :groupId and e.academicPeriodId = :periodId "
                        + "order by e.enrolledAt",
                GroupEnrollmentEntity.class);
        query.setParameter("groupId", groupId);
        query.setParameter("periodId", periodId);
        return query.getResultList().stream().map(this::toDomain).toList();
    }

    private GroupEnrollmentEntity toEntity(GroupEnrollment enrollment)
    {
        return new GroupEnrollmentEntity(enrollment.id(), enrollment.studentId(),
                enrollment.groupId(), enrollment.academicPeriodId(), enrollment.enrolledAt());
    }

    private GroupEnrollment toDomain(GroupEnrollmentEntity entity)
    {
        return new GroupEnrollment(entity.getId(), entity.getStudentId(), entity.getGroupId(),
                entity.getAcademicPeriodId(), entity.getEnrolledAt());
    }

    public boolean isDuplicateConstraintViolation(RuntimeException exception)
    {
        Throwable cause = exception;
        while (cause != null)
        {
            String message = cause.getMessage();
            if (message != null && (message.contains("uq_group_enrollment")
                    || message.contains("group_enrollment_student_id_group_id_academic_period_id_key")))
            {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
