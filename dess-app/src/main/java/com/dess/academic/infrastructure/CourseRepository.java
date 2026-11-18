package com.dess.academic.infrastructure;

import com.dess.academic.domain.Course;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CourseRepository 
{

    @PersistenceContext
    EntityManager em;

    public CourseRepository() 
    {
        
    }

    public CourseRepository(EntityManager em) 
    { 
        this.em = em; 
    }

    public void save(Course course) 
    { 
        em.persist(course); 
    }

    public Optional<Course> findById(UUID id) 
    {
        return Optional.ofNullable(em.find(Course.class, id));
    }

    public Optional<Course> findByCode(String code) 
    {
        return em.createQuery("select c from Course c where c.code = :code", Course.class)
                .setParameter("code", code)
                .getResultStream().findFirst();
    }

    public boolean existsByCode(String code) { return findByCode(code).isPresent(); }

    public List<Course> findAll() {
        return em.createQuery("select c from Course c order by c.code", Course.class).getResultList();
    }
}
