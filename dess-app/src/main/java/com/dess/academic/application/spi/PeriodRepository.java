package com.dess.academic.application.spi;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.domain.AcademicPeriod;

import java.util.List;
import java.util.Optional;

/** Port de persistance pour les périodes académiques. */
public interface PeriodRepository
{
    AcademicPeriod create(CreatePeriodCommand command);
    Optional<AcademicPeriod> findById(long id);
    List<AcademicPeriod> findAll();
}
