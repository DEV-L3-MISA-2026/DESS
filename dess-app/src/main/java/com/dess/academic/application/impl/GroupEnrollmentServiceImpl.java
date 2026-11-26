package com.dess.academic.application.impl;

import com.dess.academic.application.GroupEnrollmentService;
import com.dess.academic.domain.GroupEnrollment;
import com.dess.academic.exception.DuplicateEnrollmentException;
import com.dess.academic.exception.EnrollmentNotFoundException;
import com.dess.academic.infrastructure.GroupEnrollmentRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Implémentation applicative des inscriptions dans les groupes. */
@ApplicationScoped
@Transactional
public class GroupEnrollmentServiceImpl implements GroupEnrollmentService
{
    private final GroupEnrollmentRepository repository;

    @Inject
    public GroupEnrollmentServiceImpl(GroupEnrollmentRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public GroupEnrollment enroll(UUID studentId, UUID groupId, UUID periodId)
    {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(periodId, "academicPeriodId");

        // TODO: validate studentId through the people application interface.
        // TODO: validate groupId through StudentGroupService once available on dev.

        if (repository.exists(studentId, groupId, periodId))
        {
            throw duplicate(studentId, groupId, periodId);
        }

        try
        {
            return repository.save(GroupEnrollment.create(studentId, groupId, periodId));
        }
        catch (RuntimeException exception)
        {
            if (repository.isDuplicateConstraintViolation(exception))
            {
                throw duplicate(studentId, groupId, periodId);
            }
            throw exception;
        }
    }

    @Override
    public void remove(UUID enrollmentId)
    {
        Objects.requireNonNull(enrollmentId, "enrollmentId");
        if (repository.findById(enrollmentId).isEmpty())
        {
            throw new EnrollmentNotFoundException(enrollmentId);
        }
        repository.delete(enrollmentId);
    }

    @Override
    public GroupEnrollment get(UUID enrollmentId)
    {
        Objects.requireNonNull(enrollmentId, "enrollmentId");
        return repository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
    }

    @Override
    public List<GroupEnrollment> listByGroupAndPeriod(UUID groupId, UUID periodId)
    {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(periodId, "academicPeriodId");
        return repository.findByGroupAndPeriod(groupId, periodId);
    }

    private DuplicateEnrollmentException duplicate(UUID studentId, UUID groupId, UUID periodId)
    {
        return new DuplicateEnrollmentException(studentId, groupId, periodId);
    }
}
