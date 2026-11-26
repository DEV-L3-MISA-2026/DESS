package com.dess.academic.application;

import com.dess.academic.application.spi.ProgramEnrollmentRepository;
import com.dess.academic.domain.EnrollmentStatus;
import com.dess.academic.domain.ProgramEnrollment;
import com.dess.academic.exception.EnrollmentConflictException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake en mémoire du repository des inscriptions pour les tests unitaires. */
public class InMemoryEnrollmentRepository implements ProgramEnrollmentRepository {
    private final Map<Long, ProgramEnrollment> enrollments = new HashMap<>();
    private long nextId = 1L;

    @Override
    public ProgramEnrollment create(ProgramEnrollment enrollment) {
        if (hasActiveEnrollment(enrollment.studentId(), enrollment.programVersionId())) {
            throw new EnrollmentConflictException("Une inscription ACTIVE existe déjà pour cet étudiant et cette version");
        }
        ProgramEnrollment created = new ProgramEnrollment(nextId++, enrollment.studentId(),
                enrollment.programVersionId(), enrollment.startDate(), enrollment.endDate(),
                EnrollmentStatus.ACTIVE);
        enrollments.put(created.id(), created);
        return created;
    }

    @Override
    public Optional<ProgramEnrollment> findById(long id) {
        return Optional.ofNullable(enrollments.get(id));
    }

    @Override
    public ProgramEnrollment update(ProgramEnrollment enrollment) {
        enrollments.put(enrollment.id(), enrollment);
        return enrollment;
    }

    @Override
    public List<ProgramEnrollment> findByStudentId(long studentId) {
        return enrollments.values().stream()
                .filter(enrollment -> enrollment.studentId() == studentId)
                .toList();
    }

    @Override
    public boolean hasActiveEnrollment(long studentId, long programVersionId) {
        return enrollments.values().stream().anyMatch(enrollment
                -> enrollment.studentId() == studentId
                && enrollment.programVersionId() == programVersionId
                && enrollment.status() == EnrollmentStatus.ACTIVE);
    }
}
