package com.dess.academic.application;

/** Données de création d'une période académique. */
public record CreatePeriodCommand(String academicYear, int term) {
    public CreatePeriodCommand {
        if (academicYear == null || !academicYear.matches("[0-9]{4}-[0-9]{4}")) {
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
