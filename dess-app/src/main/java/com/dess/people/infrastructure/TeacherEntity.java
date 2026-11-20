package com.dess.people.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Entité JPA de la spécialisation teacher. */
@Entity
@Table(name = "teacher")
public class TeacherEntity {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "id")
    private AppUserEntity person;

    @Column(name = "academic_rank", nullable = false, length = 50)
    private String academicRank;

    protected TeacherEntity() {}

    public TeacherEntity(AppUserEntity person, String academicRank) {
        this.person = person;
        this.academicRank = academicRank;
    }

    public Long getId() { return id; }
    public String getAcademicRank() { return academicRank; }
}
