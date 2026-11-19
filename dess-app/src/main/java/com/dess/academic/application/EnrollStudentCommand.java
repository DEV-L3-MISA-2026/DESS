package com.dess.academic.application;

import java.time.LocalDate;

/** Données d'inscription d'un étudiant à une version de programme. */
public record EnrollStudentCommand(long studentId, long programVersionId, LocalDate startDate, LocalDate endDate) {}
