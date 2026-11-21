package com.dess.platform.web.view;

import java.util.Objects;

/** One program row on the academic page. */
public final class ProgramRow
{
    private final String code;
    private final String name;
    private final Badge status;

    /**
     * @param code program code
     * @param name program name
     * @param active whether the program is active
     */
    public ProgramRow(String code, String name, boolean active)
    {
        this.code = Objects.requireNonNull(code, "code is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.status = Badge.active(active);
    }

    public String getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }

    public Badge getStatus()
    {
        return status;
    }
}
