package com.dess.academic.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/** Période académique immuable. */
public record AcademicPeriod(Long id, String academicYear, int term) {
    private static final Pattern YEAR_PATTERN = Pattern.compile("[0-9]{4}-[0-9]{4}");

    public AcademicPeriod {
        Objects.requireNonNull(academicYear, "academicYear");
        if (!YEAR_PATTERN.matcher(academicYear).matches()) {
            throw new IllegalArgumentException("L'année universitaire doit être au format 2026-2027");
        }
        String[] years = academicYear.split("-");
        if (Integer.parseInt(years[1]) != Integer.parseInt(years[0]) + 1) {
            throw new IllegalArgumentException("L'année universitaire doit couvrir deux années consécutives");
        }
        if (term < 1 || term > 3) {
            throw new IllegalArgumentException("Le trimestre doit être compris entre 1 et 3");
        }
    }
}
