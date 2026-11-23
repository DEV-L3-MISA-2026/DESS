package com.dess.academic.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Inscription d'un étudiant dans une cohorte pour une période académique. */
public record GroupEnrollment(UUID id, UUID studentId, UUID groupId,
                              UUID academicPeriodId, Instant enrolledAt)
{
    public GroupEnrollment
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId");
        Objects.requireNonNull(enrolledAt, "enrolledAt");
    }

    public static GroupEnrollment create(UUID studentId, UUID groupId, UUID academicPeriodId)
    {
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(academicPeriodId, "academicPeriodId");
        return new GroupEnrollment(UUID.randomUUID(), studentId, groupId,
                academicPeriodId, Instant.now());
    }
}
