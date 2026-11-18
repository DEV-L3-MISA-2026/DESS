package com.dess.academic.domain;

import java.util.Objects;

/** Programme académique immuable. */
public record AcademicProgram(Long id, String code, String name, AcademicProgramStatus status) {
    public AcademicProgram {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        if (code.isBlank()) throw new IllegalArgumentException("Le code du programme est obligatoire");
        if (name.isBlank()) throw new IllegalArgumentException("Le nom du programme est obligatoire");
    }
}
