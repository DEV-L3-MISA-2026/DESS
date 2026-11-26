package com.dess.academic.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Données nécessaires à la création d'un groupe d'étudiants. */
public record CreateStudentGroupCommand(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 200) String name,
        @NotBlank String owningUnitId,
        @Positive long academicPeriodId)
{
}
