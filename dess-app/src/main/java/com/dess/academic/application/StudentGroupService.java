package com.dess.academic.application;

import com.dess.academic.domain.StudentGroup;
import com.dess.academic.exception.DuplicateStudentGroupException;
import com.dess.academic.infrastructure.StudentGroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

/** Application service for student cohorts. */
@ApplicationScoped
public class StudentGroupService {
    private final StudentGroupRepository repository;

    @Inject
    public StudentGroupService(StudentGroupRepository repository) {
        this.repository = repository;
    }

    public StudentGroupService() {
        this.repository = null;
    }

    @Transactional
    public StudentGroup create(String code, String name, UUID owningUnitId, UUID periodId) {
        if (owningUnitId == null) {
            throw new IllegalArgumentException("L'identifiant de l'unité propriétaire est obligatoire");
        }
        if (periodId == null) {
            throw new IllegalArgumentException("L'identifiant de la période académique est obligatoire");
        }

        String normalizedCode = normalizeCode(code);
        String normalizedName = requireText(name, "Le nom du groupe est obligatoire");
        if (repository.existsByCodeAndPeriod(normalizedCode, periodId)) {
            throw new DuplicateStudentGroupException(normalizedCode, periodId);
        }
        return repository.save(new StudentGroup(normalizedCode, normalizedName,
                owningUnitId, periodId));
    }

    public List<StudentGroup> listByPeriod(UUID periodId) {
        if (periodId == null) {
            throw new IllegalArgumentException("L'identifiant de la période académique est obligatoire");
        }
        return repository.findByPeriod(periodId);
    }

    private static String normalizeCode(String code) {
        return requireText(code, "Le code du groupe est obligatoire").toUpperCase();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
