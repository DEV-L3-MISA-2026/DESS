package com.dess.academic.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Version immuable d'un programme académique. */
public record ProgramVersion(Long id, long programId, String code, LocalDate effectiveFrom,
                             LocalDate effectiveTo, ProgramVersionStatus status) {
    public ProgramVersion {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(status, "status");
        if (code.isBlank()) throw new IllegalArgumentException("Le code de la version est obligatoire");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("La fin d'effet ne peut précéder son début");
        }
    }
}
