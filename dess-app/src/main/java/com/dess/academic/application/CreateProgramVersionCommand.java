package com.dess.academic.application;

import java.time.LocalDate;

/** Données de création d'une version de programme. */
public record CreateProgramVersionCommand(long programId, String code, LocalDate effectiveFrom, LocalDate effectiveTo) {}
