package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Candidate/eligibility view for one assessment.
 *
 * <p>The page shows two lists: the candidates the eligibility service says
 * may sit the assessment, and the candidates it says may not, with the
 * reason for each ineligible candidate. A candidate is never shown as
 * eligible unless the service said so.</p>
 */
public final class EligibilityView implements PageView
{
    private final PageChrome chrome;
    private final String assessmentTitle;
    private final String course;
    private final String type;
    private final String maximumScore;
    private final int durationMinutes;
    private final Badge status;
    private final String candidateSource;
    private final List<CandidateRow> eligibleCandidates;
    private final List<CandidateRow> ineligibleCandidates;
    private final String countLabel;
    private final String students;
    private final Map<String, String> errors;
    private final EmptyState candidatesEmpty;
    private final EmptyState eligibleEmpty;
    private final EmptyState ineligibleEmpty;

    /**
     * @param assessmentTitle title of the assessment
     * @param course course label, for example {@code MATH101 - Mathematics}
     * @param type type label, for example {@code Exam}
     * @param maximumScore maximum score as stored, without formatting
     * @param durationMinutes duration in minutes
     * @param status active or inactive badge of the assessment
     * @param candidateSource where the candidate list came from, for example
     *                        {@code Candidates from 2 exam sessions} or
     *                        {@code Explicit student list}
     * @param eligibleCandidates candidates the service found eligible
     * @param ineligibleCandidates candidates the service found ineligible, with
     *                             their reason
     * @param students submitted explicit student ids, kept after an error
     * @param errors field errors for the explicit student list, keyed by the
     *               field DOM id
     */
    public EligibilityView(String assessmentTitle, String course, String type,
                           String maximumScore, int durationMinutes, Badge status,
                           String candidateSource, List<CandidateRow> eligibleCandidates,
                           List<CandidateRow> ineligibleCandidates, String students,
                           Map<String, String> errors)
    {
        this.chrome = PageChrome.builder("Candidate eligibility", "Examinations",
                        "assessments", "/WEB-INF/views/eligibility/index.jsp")
                .description("Who may sit this assessment, as decided by the eligibility "
                        + "service. Ineligible candidates are listed with the reason, and "
                        + "are never silently included.")
                .breadcrumb("Overview", "/app")
                .section("Assessments", "/app/assessments")
                .headerAction("Back to assessments", "/app/assessments")
                .build();
        this.assessmentTitle = Objects.requireNonNull(assessmentTitle, "assessmentTitle is required");
        this.course = Objects.requireNonNull(course, "course is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.maximumScore = Objects.requireNonNull(maximumScore, "maximumScore is required");
        this.durationMinutes = durationMinutes;
        this.status = Objects.requireNonNull(status, "status is required");
        this.candidateSource = Objects.requireNonNull(candidateSource, "candidateSource is required");
        this.eligibleCandidates = List.copyOf(eligibleCandidates);
        this.ineligibleCandidates = List.copyOf(ineligibleCandidates);
        this.students = Objects.requireNonNull(students, "students is required");
        this.errors = Map.copyOf(errors);
        int total = eligibleCandidates.size() + ineligibleCandidates.size();
        this.countLabel = Views.count(eligibleCandidates.size(), "eligible candidate",
                "eligible candidates") + " of " + Views.count(total, "candidate", "candidates")
                + ". " + candidateSource + ".";
        this.candidatesEmpty = EmptyState.withAction("No candidates yet.",
                "Schedule an exam session for this assessment, or enter student person ids "
                        + "below to check them explicitly.",
                "Manage exam sessions", "/app/exam-sessions");
        this.eligibleEmpty = EmptyState.of("No eligible candidate.",
                "Every candidate checked so far is ineligible for this assessment.");
        this.ineligibleEmpty = EmptyState.of("No ineligible candidate.",
                "Every candidate checked may sit this assessment.");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public String getAssessmentTitle()
    {
        return assessmentTitle;
    }

    public String getCourse()
    {
        return course;
    }

    public String getType()
    {
        return type;
    }

    public String getMaximumScore()
    {
        return maximumScore;
    }

    public int getDurationMinutes()
    {
        return durationMinutes;
    }

    public Badge getStatus()
    {
        return status;
    }

    public String getCandidateSource()
    {
        return candidateSource;
    }

    public List<CandidateRow> getEligibleCandidates()
    {
        return eligibleCandidates;
    }

    public List<CandidateRow> getIneligibleCandidates()
    {
        return ineligibleCandidates;
    }

    public String getCountLabel()
    {
        return countLabel;
    }

    public String getStudents()
    {
        return students;
    }

    public Map<String, String> getErrors()
    {
        return errors;
    }

    public EmptyState getCandidatesEmpty()
    {
        return candidatesEmpty;
    }

    public EmptyState getEligibleEmpty()
    {
        return eligibleEmpty;
    }

    public EmptyState getIneligibleEmpty()
    {
        return ineligibleEmpty;
    }
}
