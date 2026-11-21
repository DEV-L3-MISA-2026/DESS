package com.dess.academic.infrastructure;

import jakarta.persistence.*;

/** Entité JPA de academic_program. */
@Entity
@Table(name = "academic_program")
public class AcademicProgramEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 20)
    private String status;

    protected AcademicProgramEntity() {}
    public AcademicProgramEntity(String code, String name, String status) { this.code = code; this.name = name; this.status = status; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
