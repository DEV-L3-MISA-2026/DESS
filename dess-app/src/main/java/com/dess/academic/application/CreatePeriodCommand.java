package com.dess.academic.application;

/** Données de création d'une période académique. */
public record CreatePeriodCommand(String academicYear, int term) {}
