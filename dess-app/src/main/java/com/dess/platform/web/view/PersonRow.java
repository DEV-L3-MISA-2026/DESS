package com.dess.platform.web.view;

import java.util.Objects;

/** One student row on the people page. */
public final class PersonRow
{
    private final long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String registrationNumber;
    private final Badge status;
    private final String name;

    /**
     * @param name full name
     * @param email email address
     * @param registrationNumber registration number
     * @param active whether the person record is active
     */
    public PersonRow(String name, String email, String registrationNumber, boolean active)
    {
        this(0L, name, email, registrationNumber, active);
    }

    public PersonRow(long id, String name, String email, String registrationNumber, boolean active)
    {
        this.id = id;
        String[] nameParts = name.split(" ", 2);
        this.firstName = nameParts.length > 0 ? nameParts[0] : "";
        this.lastName = nameParts.length > 1 ? nameParts[1] : "";
        this.email = Objects.requireNonNull(email, "email is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber,
                "registrationNumber is required");
        this.status = Badge.active(active);
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRegistrationNumber() { return registrationNumber; }
    public Badge getStatus() { return status; }
    public String getName() { return name; }
}
