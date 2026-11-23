package com.dess.academic.application.spi;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.domain.AcademicPeriod;
import java.util.List;

/** Port de persistance pour les périodes académiques. */
public interface PeriodRepository {
    AcademicPeriod create(CreatePeriodCommand command);
    List<AcademicPeriod> findAll();
}
