package com.dess.academic.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Entité JPA de program_version. */
@Entity
@Table(name = "program_version")
public class ProgramVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "program_id", nullable = false)
    private long programId;
    @Column(nullable = false, length = 20)
    private String code;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Column(nullable = false, length = 20)
    private String status;

    protected ProgramVersionEntity() {}
    public ProgramVersionEntity(long programId, String code, LocalDate effectiveFrom, LocalDate effectiveTo, String status) {
        this.programId = programId; this.code = code; this.effectiveFrom = effectiveFrom; this.effectiveTo = effectiveTo; this.status = status;
    }
    public Long getId() { return id; }
    public long getProgramId() { return programId; }
    public String getCode() { return code; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
