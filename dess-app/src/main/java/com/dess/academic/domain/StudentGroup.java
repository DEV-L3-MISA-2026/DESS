package com.dess.academic.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Cohort taught and examined together within an academic period. */
public record StudentGroup(
        UUID id,
        String code,
        String name,
        String owningUnitId,
        long academicPeriodId,
        boolean active,
        Instant createdAt)
{
    public StudentGroup
    {
        Objects.requireNonNull(id, "id");
        owningUnitId = requireText(owningUnitId, "L'identifiant de l'unité propriétaire est obligatoire");
        if (academicPeriodId <= 0)
        {
            throw new IllegalArgumentException("L'identifiant de la période académique est obligatoire");
        }
        code = normalizeCode(code);
        name = requireText(name, "Le nom du groupe est obligatoire");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public StudentGroup(String code, String name, String owningUnitId, long academicPeriodId)
    {
        this(UUID.randomUUID(), code, name, owningUnitId, academicPeriodId,
                true, Instant.now());
    }

    public StudentGroup renamed(String newName)
    {
        return new StudentGroup(id, code, newName, owningUnitId,
                academicPeriodId, active, createdAt);
    }

    public StudentGroup deactivated()
    {
        return new StudentGroup(id, code, name, owningUnitId,
                academicPeriodId, false, createdAt);
    }

    public static String normalizeCode(String code)
    {
        return requireText(code, "Le code du groupe est obligatoire").toUpperCase();
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
