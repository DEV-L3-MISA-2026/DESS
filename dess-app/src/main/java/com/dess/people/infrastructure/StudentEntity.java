package com.dess.people.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Entité JPA de la spécialisation student. */
@Entity
@Table(name = "student")
public class StudentEntity {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "id")
    private AppUserEntity person;

    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    protected StudentEntity() {}

    public StudentEntity(AppUserEntity person, String registrationNumber) {
        this.person = person;
        this.registrationNumber = registrationNumber;
    }

    public Long getId() { return id; }
    public AppUserEntity getPerson() { return person; }
    public String getRegistrationNumber() { return registrationNumber; }
}
