package com.dess.academic.application;

import com.dess.academic.application.spi.PeriodRepository;
import com.dess.academic.domain.StudentGroup;
import com.dess.academic.exception.DuplicateStudentGroupException;
import com.dess.academic.infrastructure.StudentGroupRepository;
import com.dess.institution.application.HierarchyService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.function.Predicate;

/** Application service for student cohorts. */
@ApplicationScoped
public class StudentGroupService
{
    private final StudentGroupRepository repository;
    private final HierarchyService hierarchyService;
    private final PeriodRepository periodRepository;
    private final Predicate<String> unitExists;

    @Inject
    public StudentGroupService(StudentGroupRepository repository,
                               HierarchyService hierarchyService,
                               PeriodRepository periodRepository)
    {
        this(repository, hierarchyService, periodRepository,
                unitId -> hierarchyService.getById(unitId).isPresent());
    }

    public StudentGroupService(StudentGroupRepository repository,
                        HierarchyService hierarchyService,
                        PeriodRepository periodRepository,
                        Predicate<String> unitExists)
    {
        this.repository = repository;
        this.hierarchyService = hierarchyService;
        this.periodRepository = periodRepository;
        this.unitExists = unitExists;
    }

    /** Constructor kept for isolated repository tests. */
    public StudentGroupService(StudentGroupRepository repository)
    {
        this(repository, null, null, unitId -> true);
    }

    public StudentGroup create(String code, String name,
                               String owningUnitId, long periodId)
    {
        String normalizedCode = StudentGroup.normalizeCode(code);
        String normalizedName = requireText(name, "Le nom du groupe est obligatoire");

        if (!unitExists.test(owningUnitId))
        {
            throw new IllegalArgumentException("Unité propriétaire introuvable : " + owningUnitId);
        }
        if (periodRepository != null && periodRepository.findById(periodId).isEmpty())
        {
            throw new IllegalArgumentException("Période académique introuvable : " + periodId);
        }
        if (repository.existsByCodeAndPeriod(normalizedCode, periodId))
        {
            throw new DuplicateStudentGroupException(normalizedCode, periodId);
        }
        return repository.save(new StudentGroup(normalizedCode, normalizedName,
                owningUnitId, periodId));
    }

    public List<StudentGroup> listByPeriod(long periodId)
    {
        if (periodId <= 0)
        {
            throw new IllegalArgumentException("L'identifiant de la période académique est obligatoire");
        }
        return repository.findByPeriod(periodId);
    }

    private static String requireText(String value, String message)
    {
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
