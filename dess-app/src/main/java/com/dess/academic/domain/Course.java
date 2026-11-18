package com.dess.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "course")
public class Course 
{

    @Id
    private UUID id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Course() 
    { 

    }

    public Course(String code, String title) 
    {
        this.id = UUID.randomUUID();
        this.code = normalizeCode(code);
        this.title = requireText(title, "title");
        this.createdAt = Instant.now();
    }

    public void rename(String newTitle)
    { 
        this.title = requireText(newTitle, "title"); 
    }
    public void deactivate() 
    { 
        this.active = false; 
    }

    public static String normalizeCode(String code) 
    {
        return requireText(code, "code").toUpperCase();
    }

    private static String requireText(String value, String field) 
    {
        if (value == null || value.isBlank()) 
        {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() 
    { 
        return id; 
    }
    public String getCode() 
    { 
        return code; 
    }
    public String getTitle() 
    { 
        return title; 
    }
    public boolean isActive() 
    { 
        return active; 
    }
    public Instant getCreatedAt() 
    { 
        return createdAt; 
    }

    @Override public boolean equals(Object o) 
    {
        return o instanceof Course c && Objects.equals(id, c.id);
    }
    @Override public int hashCode() 
    { 
        return Objects.hash(id); 
    }
}