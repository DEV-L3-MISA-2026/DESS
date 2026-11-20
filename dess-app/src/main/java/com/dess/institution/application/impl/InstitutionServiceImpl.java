package com.dess.institution.application.impl;

import com.dess.institution.application.ConfigurationStatus;
import com.dess.institution.application.InstitutionInfo;
import com.dess.institution.application.InstitutionService;
import com.dess.institution.application.ValidationReport;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.validation.InstitutionValidator;

import java.util.Objects;
import java.util.UUID;

/**
 * @brief Default implementation of {@link InstitutionService}.
 *
 * <p>This service exposes the institution information and the configuration lifecycle. Writes are
 * delegated to {@link ConfigurationHolder}, which validates and persists every new state.</p>
 */
public final class InstitutionServiceImpl implements InstitutionService
{
    private final ConfigurationHolder holder;

    /**
     * Creates the service.
     *
     * @param holder configuration holder shared by the module
     * @throws NullPointerException if {@code holder} is {@code null}
     */
    public InstitutionServiceImpl(ConfigurationHolder holder)
    {
        this.holder = Objects.requireNonNull(holder, "holder");
    }

    @Override
    public ConfigurationStatus getStatus()
    {
        return holder.isInitialized()
            ? ConfigurationStatus.READY
            : ConfigurationStatus.NOT_INITIALIZED;
    }

    @Override
    public InstitutionInfo initialize(String code, String name, String description)
    {
        InstitutionConfig configuration = InstitutionConfig.empty(
            UUID.randomUUID().toString(),
            code,
            name,
            description
        );

        holder.initialize(configuration);

        return toInfo(configuration);
    }

    @Override
    public InstitutionConfig getConfig()
    {
        return holder.current();
    }

    @Override
    public InstitutionInfo getInfo()
    {
        return toInfo(holder.current());
    }

    @Override
    public InstitutionInfo updateInfo(String code, String name, String description)
    {
        return holder.update(configuration ->
        {
            InstitutionConfig updated = configuration.withInfo(code, name, description);

            return new ConfigurationHolder.Change<>(updated, toInfo(updated));
        });
    }

    @Override
    public ValidationReport check()
    {
        return new ValidationReport(InstitutionValidator.validate(holder.current()));
    }

    @Override
    public void reload()
    {
        holder.load();
    }

    private static InstitutionInfo toInfo(InstitutionConfig configuration)
    {
        return new InstitutionInfo(
            configuration.id(),
            configuration.code(),
            configuration.name(),
            configuration.description()
        );
    }
}
