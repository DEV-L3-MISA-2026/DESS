package com.dess.people.infrastructure;

import jakarta.persistence.*;

/** Entité JPA de la spécialisation teacher. */
@Entity
@Table(name = "teacher")
public class TeacherEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "id")
    private AppUserEntity person;

    @Column(name = "academic_rank", nullable = false, length = 50)
    private String academicRank;

    protected TeacherEntity() {}
    public TeacherEntity(AppUserEntity person, String academicRank) {
        this.person = person; this.academicRank = academicRank;
    }
    public Long getId() { return id; }
    public String getAcademicRank() { return academicRank; }
}
