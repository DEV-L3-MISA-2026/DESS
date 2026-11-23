package com.dess.assessment.application.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.dess.academic.application.ProgramEnrollmentService;
import com.dess.academic.domain.EnrollmentStatus;
import com.dess.assessment.application.EligibilityService;
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.people.application.PeopleService;

/**
 * @brief Application rules of the candidate eligibility check.
 *
 * <p>A student may sit an assessment when the assessment exists and
 * is active, the student exists as a student, and the student has
 * at least one ACTIVE program enrollment. The program enrollment is
 * the enrollment rule that exists on this branch. The academic
 * module has no course offering yet, so no offering enrollment can
 * be checked. When a course offering lands, the check should also
 * require an enrollment for the offering of the assessment.</p>
 */
@ApplicationScoped
@Transactional
public class EligibilityServiceImpl implements EligibilityService
{
    private final AssessmentRepository assessments;
    private final PeopleService peopleService;
    private final ProgramEnrollmentService enrollmentService;

    @Inject
    public EligibilityServiceImpl(AssessmentRepository assessments, PeopleService peopleService,
                                  ProgramEnrollmentService enrollmentService)
    {
        this.assessments = assessments;
        this.peopleService = peopleService;
        this.enrollmentService = enrollmentService;
    }

    @Override
    public Eligibility check(UUID assessmentId, long studentPersonId)
    {
        if (assessmentId == null)
        {
            throw new IllegalArgumentException("assessmentId is required");
        }
        Assessment assessment = assessments.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(
                        "assessment not found: " + assessmentId));
        List<String> reasons = new ArrayList<>();
        if (!assessment.active())
        {
            reasons.add("assessment is not active");
        }
        if (!peopleService.studentExists(studentPersonId))
        {
            reasons.add("student not found: " + studentPersonId);
        }
        else if (enrollmentService.listByStudent(studentPersonId).stream()
                .noneMatch(enrollment -> enrollment.status() == EnrollmentStatus.ACTIVE))
        {
            reasons.add("student has no active program enrollment");
        }
        return new Eligibility(reasons.isEmpty(), List.copyOf(reasons));
    }

    @Override
    public List<Long> eligibleStudents(UUID assessmentId, List<Long> studentPersonIds)
    {
        if (studentPersonIds == null)
        {
            throw new IllegalArgumentException("studentPersonIds are required");
        }
        return studentPersonIds.stream()
                .filter(studentPersonId -> check(assessmentId, studentPersonId).eligible())
                .toList();
    }
}
