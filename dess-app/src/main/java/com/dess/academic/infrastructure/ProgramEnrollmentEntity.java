package com.dess.academic.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Entité JPA de program_enrollment. */
@Entity
@Table(name = "program_enrollment")
public class ProgramEnrollmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id", nullable = false)
    private long studentId;
    @Column(name = "program_version_id", nullable = false)
    private long programVersionId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(nullable = false, length = 20)
    private String status;

    protected ProgramEnrollmentEntity() {}
    public ProgramEnrollmentEntity(long studentId, long programVersionId, LocalDate startDate, LocalDate endDate, String status) {
        this.studentId = studentId; this.programVersionId = programVersionId; this.startDate = startDate; this.endDate = endDate; this.status = status;
    }
    public Long getId() { return id; }
    public long getStudentId() { return studentId; }
    public long getProgramVersionId() { return programVersionId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setStatus(String status) { this.status = status; }
}
