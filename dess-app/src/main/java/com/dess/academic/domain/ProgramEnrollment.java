package com.dess.academic.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Inscription immuable d'un étudiant à une version de programme. */
public record ProgramEnrollment(Long id, long studentId, long programVersionId,
                                LocalDate startDate, LocalDate endDate, EnrollmentStatus status) {
    public ProgramEnrollment {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(status, "status");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fin d'inscription ne peut précéder son début");
        }
    }
}
