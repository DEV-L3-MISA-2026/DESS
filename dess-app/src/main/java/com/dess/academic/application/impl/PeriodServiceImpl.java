package com.dess.academic.application.impl;

import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.PeriodService;
import com.dess.academic.application.spi.PeriodRepository;
import com.dess.academic.domain.AcademicPeriod;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/** Implémentation applicative des périodes académiques. */
@ApplicationScoped
@Transactional
public class PeriodServiceImpl implements PeriodService {
    private final PeriodRepository repository;

    @Inject
    public PeriodServiceImpl(PeriodRepository repository) {
        this.repository = repository;
    }

    @Override
    public AcademicPeriod create(CreatePeriodCommand command) {
        if (command == null) throw new IllegalArgumentException("La commande est obligatoire");
        return repository.create(command);
    }

    @Override
    public List<AcademicPeriod> listAll() {
        return repository.findAll();
    }
}
