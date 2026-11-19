package com.dess.academic.infrastructure;

import com.dess.academic.domain.StudentGroup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StudentGroupRepository {

    @PersistenceContext
    EntityManager em;

    public StudentGroupRepository() {
    }

    /** Pour les tests hors conteneur. */
    public StudentGroupRepository(EntityManager em) {
        this.em = em;
    }

    public void save(StudentGroup studentGroup) {
        em.persist(studentGroup);
    }

    public Optional<StudentGroup> findById(UUID id) {
        return Optional.ofNullable(em.find(StudentGroup.class, id));
    }

    public boolean existsByCodeAndPeriod(String code, UUID periodId) {
        Long count = em.createQuery(
                        "select count(s) from StudentGroup s where s.code = :code and s.academicPeriodId = :periodId",
                        Long.class)
                .setParameter("code", code)
                .setParameter("periodId", periodId)
                .getSingleResult();
        return count > 0;
    }

    public List<StudentGroup> findByPeriod(UUID periodId) {
        return em.createQuery(
                        "select s from StudentGroup s where s.academicPeriodId = :periodId order by s.code",
                        StudentGroup.class)
                .setParameter("periodId", periodId)
                .getResultList();
    }
}
