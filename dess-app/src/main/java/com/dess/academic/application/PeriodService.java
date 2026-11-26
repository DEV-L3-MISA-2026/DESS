package com.dess.academic.application;

import com.dess.academic.domain.AcademicPeriod;
import java.util.List;

/** API applicative des périodes académiques. */
public interface PeriodService {
    AcademicPeriod create(CreatePeriodCommand command);
    List<AcademicPeriod> listAll();
}
