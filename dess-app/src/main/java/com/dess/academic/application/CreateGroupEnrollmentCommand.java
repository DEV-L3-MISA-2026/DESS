package com.dess.academic.application;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Données nécessaires à une inscription dans un groupe. */
public record CreateGroupEnrollmentCommand(
        @NotNull UUID studentId,
        @NotNull UUID groupId,
        @NotNull UUID academicPeriodId)
{
}
