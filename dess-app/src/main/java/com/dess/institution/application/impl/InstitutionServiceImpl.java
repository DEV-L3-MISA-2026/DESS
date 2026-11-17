package com.dess.institution.application.impl;

import com.dess.institution.application.ConfigurationStatus;
import com.dess.institution.application.InstitutionInfo;
import com.dess.institution.application.InstitutionService;
import com.dess.institution.application.ValidationReport;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.validation.InstitutionValidator;

import java.util.UUID;

public final class InstitutionServiceImpl implements InstitutionService {

    private final ConfigurationHolder holder;

    public InstitutionServiceImpl(ConfigurationHolder holder) {
        this.holder = holder;
    }

    @Override
    public ConfigurationStatus getStatus() {
        return holder.isInitialized() ? ConfigurationStatus.READY : ConfigurationStatus.NOT_INITIALIZED;
    }

    @Override
    public InstitutionInfo initialize(String code, String name, String description) {
        InstitutionConfig config = InstitutionConfig.empty(UUID.randomUUID().toString(), code, name, description);
        holder.initialize(config);
        return toInfo(config);
    }

    @Override
    public InstitutionConfig getConfig() {
        return holder.current();
    }

    @Override
    public InstitutionInfo getInfo() {
        return toInfo(holder.current());
    }

    @Override
    public InstitutionInfo updateInfo(String code, String name, String description) {
        return holder.update(cfg -> {
            InstitutionConfig updated = cfg.withInfo(code, name, description);
            return new ConfigurationHolder.Change<>(updated, toInfo(updated));
        });
    }

    @Override
    public ValidationReport check() {
        return new ValidationReport(InstitutionValidator.validate(holder.current()));
    }

    @Override
    public void reload() {
        holder.load();
    }

    private static InstitutionInfo toInfo(InstitutionConfig c) {
        return new InstitutionInfo(c.id(), c.code(), c.name(), c.description());
    }
}
