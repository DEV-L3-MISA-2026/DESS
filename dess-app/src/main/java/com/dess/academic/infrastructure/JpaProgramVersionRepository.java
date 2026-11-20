package com.dess.academic.infrastructure;

import com.dess.academic.application.CreateProgramVersionCommand;
import com.dess.academic.application.spi.ProgramVersionRepository;
import com.dess.academic.domain.ProgramVersion;
import com.dess.academic.domain.ProgramVersionStatus;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.ProgramNotFoundException;
import com.dess.academic.exception.ProgramVersionNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;

/** Implémentation JPA du port des versions de programme. */
@ApplicationScoped
public class JpaProgramVersionRepository implements ProgramVersionRepository {
    @PersistenceContext(unitName = "dessPU")
    private EntityManager entityManager;

    public JpaProgramVersionRepository() {}

    public JpaProgramVersionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ProgramVersion create(CreateProgramVersionCommand command) {
        ProgramVersion domain = new ProgramVersion(null, command.programId(), command.code().trim(),
                command.effectiveFrom(), command.effectiveTo(), ProgramVersionStatus.DRAFT);
        if (entityManager.find(AcademicProgramEntity.class, command.programId()) == null) {
            throw new ProgramNotFoundException("Programme introuvable : " + command.programId());
        }
        try {
            ProgramVersionEntity entity = new ProgramVersionEntity(command.programId(), domain.code(),
                    domain.effectiveFrom(), domain.effectiveTo(), domain.status().name());
            entityManager.persist(entity);
            entityManager.flush();
            return toDomain(entity);
        } catch (PersistenceException exception) {
            throw new AcademicException("Impossible de créer la version : code dupliqué ou dates invalides", exception);
        }
    }

    @Override
    public Optional<ProgramVersion> findById(long id) {
        return Optional.ofNullable(entityManager.find(ProgramVersionEntity.class, id)).map(this::toDomain);
    }

    @Override
    public ProgramVersion updateStatus(long id, ProgramVersionStatus status) {
        ProgramVersionEntity entity = entityManager.find(ProgramVersionEntity.class, id);
        if (entity == null) throw new ProgramVersionNotFoundException("Version introuvable : " + id);
        entity.setStatus(status.name());
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    public List<ProgramVersion> findByProgramId(long programId) {
        return entityManager.createQuery(
                        "select v from ProgramVersionEntity v where v.programId = :programId", ProgramVersionEntity.class)
                .setParameter("programId", programId)
                .getResultList().stream().map(this::toDomain).toList();
    }

    private ProgramVersion toDomain(ProgramVersionEntity entity) {
        return new ProgramVersion(entity.getId(), entity.getProgramId(), entity.getCode(),
                entity.getEffectiveFrom(), entity.getEffectiveTo(),
                ProgramVersionStatus.valueOf(entity.getStatus()));
    }
}
