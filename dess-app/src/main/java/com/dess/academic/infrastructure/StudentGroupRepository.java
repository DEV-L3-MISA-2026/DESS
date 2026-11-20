package com.dess.academic.infrastructure;

import com.dess.academic.domain.StudentGroup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port d'accès JPA aux cohortes étudiantes. */
@ApplicationScoped
public class StudentGroupRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public StudentGroupRepository() {}

    public StudentGroupRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public StudentGroup save(StudentGroup group) {
        return entityManager.merge(group);
    }

    public Optional<StudentGroup> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(StudentGroup.class, id));
    }

    public boolean existsByCodeAndPeriod(String code, UUID periodId) {
        return entityManager.createQuery(
                        "select count(g) from StudentGroup g where g.code = :code and g.academicPeriodId = :periodId",
                        Long.class)
                .setParameter("code", code)
                .setParameter("periodId", periodId)
                .getSingleResult() > 0;
    }

    public List<StudentGroup> findByPeriod(UUID periodId) {
        TypedQuery<StudentGroup> query = entityManager.createQuery(
                "select g from StudentGroup g where g.academicPeriodId = :periodId order by g.code",
                StudentGroup.class);
        query.setParameter("periodId", periodId);
        return query.getResultList();
    }
}
