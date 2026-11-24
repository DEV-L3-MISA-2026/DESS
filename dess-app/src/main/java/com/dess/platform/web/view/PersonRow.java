package com.dess.platform.web.view;

import java.util.Objects;

/** One student row on the people page. */
public final class PersonRow
{
    private final String name;
    private final String email;
    private final String registrationNumber;
    private final Badge status;

    /**
     * @param name full name
     * @param email email address
     * @param registrationNumber registration number
     * @param active whether the person record is active
     */
    public PersonRow(String name, String email, String registrationNumber, boolean active)
    {
        this.name = Objects.requireNonNull(name, "name is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber,
                "registrationNumber is required");
        this.status = Badge.active(active);
    }

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    public String getRegistrationNumber()
    {
        return registrationNumber;
    }

    public Badge getStatus()
    {
        return status;
    }
}
