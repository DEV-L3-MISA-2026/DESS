package com.dess.academic.application;

import com.dess.academic.application.impl.ProgramEnrollmentServiceImpl;
import com.dess.academic.domain.*;
import com.dess.academic.exception.EnrollmentConflictException;
import com.dess.academic.exception.InvalidEnrollmentException;
import com.dess.academic.infrastructure.AcademicRepository;
import com.dess.people.application.PeopleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires des règles d'inscription aux programmes académiques. */
class ProgramEnrollmentServiceImplTest {
    private InMemoryAcademicRepository academicRepository;
    private PeopleService peopleService;
    private ProgramEnrollmentServiceImpl service;

    @BeforeEach
    void setUp() {
        academicRepository = new InMemoryAcademicRepository();
        peopleService = new PeopleServiceFake();
        service = new ProgramEnrollmentServiceImpl(academicRepository, peopleService);

        academicRepository.program = new AcademicProgram(10L, "LICENCE", "Licence", AcademicProgramStatus.ACTIVE);
        academicRepository.version = new ProgramVersion(20L, 10L, "2026",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30), ProgramVersionStatus.ACTIVE);
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
        academicRepository.version = new ProgramVersion(20L, 10L, "2026",
                LocalDate.of(2026, 9, 1), null, ProgramVersionStatus.DRAFT);
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null)));

        academicRepository.version = new ProgramVersion(20L, 10L, "2026",
                LocalDate.of(2026, 9, 1), null, ProgramVersionStatus.ARCHIVED);
        assertThrows(InvalidEnrollmentException.class, () -> service.enroll(new EnrollStudentCommand(1L, 20L,
                LocalDate.of(2026, 9, 1), null)));
    }

    @Test
    void rejectsInactiveProgram() {
        academicRepository.program = new AcademicProgram(10L, "LICENCE", "Licence", AcademicProgramStatus.INACTIVE);
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

    private static class PeopleServiceFake implements PeopleService {
        @Override public com.dess.people.domain.StudentProfile createStudent(com.dess.people.application.CreateStudentCommand command) { throw new UnsupportedOperationException(); }
        @Override public com.dess.people.domain.TeacherProfile createTeacher(com.dess.people.application.CreateTeacherCommand command) { throw new UnsupportedOperationException(); }
        @Override public Optional<com.dess.people.domain.Person> findById(long id) { return Optional.empty(); }
        @Override public Optional<com.dess.people.domain.Person> findByEmail(String email) { return Optional.empty(); }
        @Override public List<com.dess.people.domain.StudentProfile> listStudents() { return List.of(); }
        @Override public boolean studentExists(long id) { return id == 1L; }
    }

    private static class InMemoryAcademicRepository implements AcademicRepository {
        private AcademicProgram program;
        private ProgramVersion version;
        private final Map<Long, ProgramEnrollment> enrollments = new HashMap<>();
        private long nextEnrollmentId = 1L;

        @Override public AcademicPeriod createPeriod(CreatePeriodCommand command) { throw new UnsupportedOperationException(); }
        @Override public List<AcademicPeriod> listPeriods() { return List.of(); }
        @Override public AcademicProgram createProgram(CreateProgramCommand command) { throw new UnsupportedOperationException(); }
        @Override public Optional<AcademicProgram> findProgram(long programId) { return Optional.ofNullable(program); }
        @Override public AcademicProgram saveProgram(AcademicProgram program) { this.program = program; return program; }
        @Override public List<AcademicProgram> listPrograms() { return program == null ? List.of() : List.of(program); }
        @Override public ProgramVersion createVersion(CreateProgramVersionCommand command) { throw new UnsupportedOperationException(); }
        @Override public Optional<ProgramVersion> findVersion(long programVersionId) { return Optional.ofNullable(version); }
        @Override public ProgramVersion saveVersion(ProgramVersion version) { this.version = version; return version; }
        @Override public List<ProgramVersion> listVersions(long programId) { return version == null ? List.of() : List.of(version); }
        @Override public ProgramEnrollment createEnrollment(EnrollStudentCommand command) {
            ProgramEnrollment enrollment = new ProgramEnrollment(nextEnrollmentId++, command.studentId(), command.programVersionId(),
                    command.startDate(), command.endDate(), EnrollmentStatus.ACTIVE);
            enrollments.put(enrollment.id(), enrollment);
            return enrollment;
        }
        @Override public Optional<ProgramEnrollment> findEnrollment(long enrollmentId) { return Optional.ofNullable(enrollments.get(enrollmentId)); }
        @Override public ProgramEnrollment saveEnrollment(ProgramEnrollment enrollment) { enrollments.put(enrollment.id(), enrollment); return enrollment; }
        @Override public List<ProgramEnrollment> listEnrollmentsByStudent(long studentId) { return enrollments.values().stream().filter(e -> e.studentId() == studentId).toList(); }
        @Override public boolean hasActiveEnrollment(long studentId, long programVersionId) {
            return enrollments.values().stream().anyMatch(e -> e.studentId() == studentId
                    && e.programVersionId() == programVersionId && e.status() == EnrollmentStatus.ACTIVE);
        }
    }
}
