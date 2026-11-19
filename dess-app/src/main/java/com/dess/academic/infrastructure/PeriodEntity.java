package com.dess.academic.infrastructure;

import jakarta.persistence.*;

/** Entité JPA de la table period. */
@Entity
@Table(name = "period")
public class PeriodEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;
    @Column(nullable = false)
    private int term;

    protected PeriodEntity() {}
    public PeriodEntity(String academicYear, int term) { this.academicYear = academicYear; this.term = term; }
    public Long getId() { return id; }
    public String getAcademicYear() { return academicYear; }
    public int getTerm() { return term; }
}
