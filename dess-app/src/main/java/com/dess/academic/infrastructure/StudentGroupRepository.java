package com.dess.academic.infrastructure;

import com.dess.academic.domain.StudentGroup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port d'accès JPA aux cohortes étudiantes. */
@ApplicationScoped
@Transactional
public class StudentGroupRepository
{
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public StudentGroupRepository() {}

    public StudentGroupRepository(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    public StudentGroup save(StudentGroup group)
    {
        return toGroup(entityManager.merge(toEntity(group)));
    }

    public Optional<StudentGroup> findById(UUID id)
    {
        return Optional.ofNullable(entityManager.find(StudentGroupEntity.class, id)).map(this::toGroup);
    }

    public boolean existsByCodeAndPeriod(String code, long periodId)
    {
        return entityManager.createQuery(
                        "select count(g) from StudentGroupEntity g where g.code = :code and g.academicPeriodId = :periodId",
                        Long.class)
                .setParameter("code", code)
                .setParameter("periodId", periodId)
                .getSingleResult() > 0;
    }

    public List<StudentGroup> findByPeriod(long periodId)
    {
        TypedQuery<StudentGroupEntity> query = entityManager.createQuery(
                "select g from StudentGroupEntity g where g.academicPeriodId = :periodId order by g.code",
                StudentGroupEntity.class);
        query.setParameter("periodId", periodId);
        return query.getResultList().stream().map(this::toGroup).toList();
    }

    private StudentGroupEntity toEntity(StudentGroup group)
    {
        return new StudentGroupEntity(group.id(), group.code(), group.name(),
                group.owningUnitId(), group.academicPeriodId(),
                group.active(), group.createdAt());
    }

    private StudentGroup toGroup(StudentGroupEntity entity)
    {
        return new StudentGroup(entity.getId(), entity.getCode(), entity.getName(),
                entity.getOwningUnitId(), entity.getAcademicPeriodId(),
                entity.isActive(), entity.getCreatedAt());
    }
}
