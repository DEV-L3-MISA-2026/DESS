package com.dess.people.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Entité JPA de la table app_user. */
@Entity
@Table(name = "app_user")
public class AppUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private boolean active = true;

    protected AppUserEntity() {}

    public AppUserEntity(String email, String lastName, String firstName, String role) {
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }

    public void setEmail(String email) { this.email = email; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setActive(boolean active) { this.active = active; }
}
