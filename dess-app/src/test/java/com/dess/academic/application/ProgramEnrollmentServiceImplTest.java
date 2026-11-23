package com.dess.academic.application;

import com.dess.academic.application.impl.ProgramEnrollmentServiceImpl;
import com.dess.academic.domain.*;
import com.dess.academic.exception.EnrollmentConflictException;
import com.dess.academic.exception.InvalidEnrollmentException;
import com.dess.people.application.PeopleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires des règles d'inscription aux programmes académiques. */
class ProgramEnrollmentServiceImplTest {
    private InMemoryEnrollmentRepository enrollmentRepository;
    private InMemoryProgramService programService;
    private PeopleService peopleService;
    private ProgramEnrollmentServiceImpl service;

    @BeforeEach
    void setUp() {
        enrollmentRepository = new InMemoryEnrollmentRepository();
        programService = new InMemoryProgramService();
        peopleService = new InMemoryPeopleService(true);
        service = new ProgramEnrollmentServiceImpl(enrollmentRepository, programService, peopleService);
    }

    @Test
    void enrollsStudentInActiveVersion() {
        ProgramEnrollment enrollment = service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null));
        assertEquals(1L, enrollment.studentId());
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.status());
    }

    @Test
    void rejectsUnknownStudent() {
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(999L, 20L,
                LocalDate.of(2026, 9, 1), null)));
    }

    @Test
    void rejectsDraftOrArchivedVersion() {
        programService.setVersion(new ProgramVersion(20L, 10L, "2026",
                LocalDate.of(2026, 9, 1), null, ProgramVersionStatus.DRAFT));
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null)));

        programService.setVersion(new ProgramVersion(20L, 10L, "2026",
                LocalDate.of(2026, 9, 1), null, ProgramVersionStatus.ARCHIVED));
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null)));
    }

    @Test
    void rejectsInactiveProgram() {
        programService.setProgram(new AcademicProgram(10L, "LICENCE", "Licence", AcademicProgramStatus.INACTIVE));
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null)));
    }

    @Test
    void rejectsInconsistentAndOutOfEffectDates() {
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1))));
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 8, 1), null)));
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 1))));
    }

    @Test
    void rejectsSecondActiveEnrollmentForSameStudentAndVersion() {
        service.enroll(new EnrollStudentCommand(1L, 20L, LocalDate.of(2026, 9, 1), null));
        assertThrows(EnrollmentConflictException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 2), null)));
    }

    @Test
    void completesActiveEnrollment() {
        ProgramEnrollment enrollment = service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null));

        ProgramEnrollment completed = service.complete(enrollment.id(), LocalDate.of(2027, 6, 30));
        assertEquals(EnrollmentStatus.COMPLETED, completed.status());

        assertThrows(InvalidEnrollmentException.class,
                () -> service.withdraw(completed.id(), LocalDate.of(2027, 6, 30)));
        assertThrows(InvalidEnrollmentException.class,
                () -> service.complete(completed.id(), LocalDate.of(2027, 6, 30)));
    }

    @Test
    void withdrawsActiveEnrollment() {
        ProgramEnrollment enrollment = service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null));

        ProgramEnrollment withdrawn = service.withdraw(enrollment.id(), LocalDate.of(2026, 12, 1));
        assertEquals(EnrollmentStatus.WITHDRAWN, withdrawn.status());
        assertThrows(InvalidEnrollmentException.class,
                () -> service.complete(withdrawn.id(), LocalDate.of(2026, 12, 1)));
    }

    @Test
    void rejectsEndDateBeforeStartDateWhenCompleting() {
        ProgramEnrollment enrollment = service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null));

        assertThrows(InvalidEnrollmentException.class,
                () -> service.complete(enrollment.id(), LocalDate.of(2026, 8, 1)));
    }
}
