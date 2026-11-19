package com.dess.academic.application;

import com.dess.academic.domain.StudentGroup;
import com.dess.academic.infrastructure.StudentGroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class StudentGroupService {

    @Inject
    StudentGroupRepository repository;

    public StudentGroupService() {
    }

    /** Pour les tests hors conteneur. */
    public StudentGroupService(StudentGroupRepository repository) {
        this.repository = repository;
    }

    public StudentGroup create(String code, String name, UUID owningUnitId, UUID periodId) {
        String normalizedCode = StudentGroup.normalizeCode(code);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (owningUnitId == null) {
            throw new IllegalArgumentException("owningUnitId must not be null");
        }
        if (periodId == null) {
            throw new IllegalArgumentException("academicPeriodId must not be null");
        }
        if (repository.existsByCodeAndPeriod(normalizedCode, periodId)) {
            throw new DuplicateStudentGroupException(normalizedCode, periodId);
        }

        StudentGroup studentGroup = new StudentGroup(normalizedCode, name, owningUnitId, periodId);
        repository.save(studentGroup);
        return studentGroup;
    }

    public List<StudentGroup> listByPeriod(UUID periodId) {
        if (periodId == null) {
            throw new IllegalArgumentException("academicPeriodId must not be null");
        }
        return repository.findByPeriod(periodId);
    }

    public static class DuplicateStudentGroupException extends RuntimeException {
        public DuplicateStudentGroupException(String code, UUID periodId) {
            super("A student group with code '" + code
                    + "' already exists for academic period '" + periodId + "'");
        }
    }
}
