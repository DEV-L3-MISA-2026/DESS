package com.dess.academic.infrastructure;

import com.dess.academic.domain.Course;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implémentation CDI/JPA du port des cours. */
@ApplicationScoped
public class JpaCourseRepository implements CourseRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaCourseRepository() {
    }

    public JpaCourseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Course save(Course course) {
        CourseEntity entity = toEntity(course);
        // merge couvre la création comme la mise à jour d'un cours existant.
        return toCourse(entityManager.merge(entity));
    }

    @Override
    public Optional<Course> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(CourseEntity.class, id)).map(this::toCourse);
    }

    @Override
    public Optional<Course> findByCode(String code) {
        TypedQuery<CourseEntity> query = entityManager.createQuery(
                "select c from CourseEntity c where c.code = :code", CourseEntity.class);
        query.setParameter("code", code);
        return query.getResultStream().findFirst().map(this::toCourse);
    }

    @Override
    public boolean existsByCode(String code) {
        return entityManager.createQuery(
                        "select count(c) from CourseEntity c where c.code = :code", Long.class)
                .setParameter("code", code)
                .getSingleResult() > 0;
    }

    @Override
    public List<Course> findAll() {
        return entityManager.createQuery(
                        "select c from CourseEntity c order by c.code", CourseEntity.class)
                .getResultList()
                .stream()
                .map(this::toCourse)
                .toList();
    }

    private CourseEntity toEntity(Course course) {
        return new CourseEntity(course.id(), course.code(), course.title(),
                course.active(), course.createdAt());
    }

    private Course toCourse(CourseEntity entity) {
        return new Course(entity.getId(), entity.getCode(), entity.getTitle(),
                entity.isActive(), entity.getCreatedAt());
    }
}
