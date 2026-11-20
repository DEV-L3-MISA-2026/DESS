package com.dess.academic.application.impl;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.PeriodService;
import com.dess.academic.domain.AcademicPeriod;
import com.dess.academic.infrastructure.AcademicRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/** Implémentation applicative des périodes académiques. */
@ApplicationScoped
@Transactional
public class PeriodServiceImpl implements PeriodService {
    private final AcademicRepository repository;

    @Inject
    public PeriodServiceImpl(AcademicRepository repository) { this.repository = repository; }

    @Override
    public AcademicPeriod create(CreatePeriodCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        new AcademicPeriod(null, command.academicYear(), command.term());
        return repository.createPeriod(command);
    }

    @Override
    public List<AcademicPeriod> listAll() { return repository.listPeriods(); }
}
