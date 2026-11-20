package com.dess.academic.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Entité JPA de la table course. */
@Entity
@Table(name = "course")
public class CourseEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CourseEntity() {}

    public CourseEntity(UUID id, String code, String title, boolean active, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
