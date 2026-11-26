package com.dess.academic.infrastructure;

import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.spi.ProgramRepository;
import com.dess.academic.domain.AcademicProgram;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.ProgramNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;

/** Implémentation JPA du port des programmes. */
@ApplicationScoped
public class JpaProgramRepository implements ProgramRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaProgramRepository() {}

    public JpaProgramRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public AcademicProgram create(CreateProgramCommand command) {
        AcademicProgram domain = new AcademicProgram(null, command.code().trim(),
                command.name().trim(), AcademicProgramStatus.ACTIVE);
        try {
            AcademicProgramEntity entity = new AcademicProgramEntity(
                    domain.code(), domain.name(), domain.status().name());
            entityManager.persist(entity);
            entityManager.flush();
            return toDomain(entity);
        } catch (PersistenceException exception) {
            throw new AcademicException("Impossible de créer le programme : code déjà utilisé", exception);
        }
    }

    @Override
    public Optional<AcademicProgram> findById(long id) {
        return Optional.ofNullable(entityManager.find(AcademicProgramEntity.class, id)).map(this::toDomain);
    }

    @Override
    public AcademicProgram updateStatus(long id, AcademicProgramStatus status) {
        AcademicProgramEntity entity = entityManager.find(AcademicProgramEntity.class, id);
        if (entity == null) throw new ProgramNotFoundException("Programme introuvable : " + id);
        entity.setStatus(status.name());
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    public List<AcademicProgram> findAll() {
        return entityManager.createQuery("select p from AcademicProgramEntity p", AcademicProgramEntity.class)
                .getResultList().stream().map(this::toDomain).toList();
    }

    private AcademicProgram toDomain(AcademicProgramEntity entity) {
        return new AcademicProgram(entity.getId(), entity.getCode(), entity.getName(),
                AcademicProgramStatus.valueOf(entity.getStatus()));
    }
}
