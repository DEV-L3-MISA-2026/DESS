package com.dess.platform.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.dess.academic.application.CourseService;
import com.dess.academic.exception.CourseNotFoundException;
import com.dess.assessment.application.AssessmentService;
import com.dess.assessment.application.EligibilityService;
import com.dess.assessment.application.ExamSessionService;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.assessment.presentation.EligibilityPresentation;
import com.dess.people.application.PeopleService;
import com.dess.platform.web.view.Badge;
import com.dess.platform.web.view.CandidateRow;
import com.dess.platform.web.view.EligibilityView;
import com.dess.platform.web.view.Views;

/**
 * @brief Candidate/eligibility view for one assessment.
 *
 * <p>The candidates come from the exam sessions of the assessment: the
 * union of every session candidate list, in session order, without
 * duplicates. When no session exists for the assessment, the page checks
 * an explicit student list given as comma-separated person ids in the
 * {@code students} query parameter, so the check stays possible before
 * any session is scheduled. The explicit list can also add candidates
 * to check alongside the session lists. Either way, a candidate is
 * shown as eligible only when the eligibility service says so, and the
 * reason shown for an ineligible candidate is the sentence from
 * {@code EligibilityPresentation}, the same wording as the API error.</p>
 */
@WebServlet("/app/eligibility")
public class EligibilityServlet extends HttpServlet
{
    @Inject
    private AssessmentService assessmentService;

    @Inject
    private ExamSessionService examSessionService;

    @Inject
    private EligibilityService eligibilityService;

    @Inject
    private PeopleService peopleService;

    @Inject
    private CourseService courseService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);

        String assessmentValue = Web.param(request, "assessment");

        if (assessmentValue.isBlank())
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Enter an assessment id.");
            return;
        }

        UUID assessmentId;

        try
        {
            assessmentId = UUID.fromString(assessmentValue);
        }
        catch (IllegalArgumentException exception)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Enter a valid assessment id.");
            return;
        }

        Assessment assessment;

        try
        {
            assessment = assessmentService.get(assessmentId);
        }
        catch (AssessmentNotFoundException exception)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
            return;
        }

        String students = Web.param(request, "students");
        CandidateIds candidates = resolveCandidateIds(examSessionService.findAll(),
                assessmentId, students);
        Map<String, String> errors = new LinkedHashMap<>();

        if (!candidates.errors().isEmpty())
        {
            errors.put("students", String.join(" ", candidates.errors()));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        List<CandidateRow> rows = buildRows(candidates.ids(), assessmentId,
                eligibilityService, peopleService);

        EligibilityView view = new EligibilityView(
                assessment.title(),
                courseLabel(assessment),
                AssessmentPresentation.typeLabel(assessment.type()),
                assessment.maximumScore().toPlainString(),
                assessment.durationMinutes(),
                Badge.active(assessment.active()),
                candidateSource(candidates),
                rows.stream().filter(CandidateRow::isEligible).toList(),
                rows.stream().filter(row -> !row.isEligible()).toList(),
                students,
                errors);

        Views.render(request, response, view);
    }

    /**
     * @brief Resolves the candidates to check for one assessment.
     *
     * @param sessions every stored exam session
     * @param assessmentId the assessment to check
     * @param explicitIds comma-separated person ids to check as well; may be
     *                    blank
     * @return the candidate ids in first-seen order, without duplicates, plus
     *         one message per id that could not be read
     */
    static CandidateIds resolveCandidateIds(List<ExamSession> sessions, UUID assessmentId,
                                            String explicitIds)
    {
        Set<Long> ids = new LinkedHashSet<>();
        int sessionCount = 0;

        for (ExamSession session : sessions)
        {
            if (!session.assessmentId().equals(assessmentId))
            {
                continue;
            }
            sessionCount++;
            ids.addAll(session.candidatePersonIds());
        }

        List<String> errors = new ArrayList<>();

        if (explicitIds != null && !explicitIds.isBlank())
        {
            for (String part : explicitIds.split(","))
            {
                String text = part.trim();
                if (text.isEmpty())
                {
                    continue;
                }
                try
                {
                    long personId = Long.parseLong(text);
                    if (personId <= 0)
                    {
                        throw new NumberFormatException("person id must be positive");
                    }
                    ids.add(personId);
                }
                catch (NumberFormatException exception)
                {
                    errors.add("Enter student person ids as whole numbers separated by commas.");
                    break;
                }
            }
        }

        return new CandidateIds(List.copyOf(ids), errors, sessionCount);
    }

    /** @brief The resolved candidate list, with read errors and its source. */
    record CandidateIds(List<Long> ids, List<String> errors, int sessionCount)
    {
    }

    private String candidateSource(CandidateIds candidates)
    {
        if (candidates.sessionCount() == 0)
        {
            return "Explicit student list";
        }
        if (candidates.ids().isEmpty())
        {
            return "No candidate yet";
        }
        String source = "Candidates from "
                + Views.count(candidates.sessionCount(), "exam session", "exam sessions");
        return source;
    }

    /**
     * @brief Builds one row per candidate from the service output.
     *
     * <p>Every row asks the eligibility service: the page never decides
     * eligibility itself, and the reason sentence comes from
     * {@code EligibilityPresentation}.</p>
     *
     * @param candidateIds person ids to check, in display order
     * @param assessmentId the assessment to check
     * @param eligibilityService the service that decides eligibility
     * @param peopleService the service that names the candidates
     * @return one row per candidate, in the given order
     */
    static List<CandidateRow> buildRows(List<Long> candidateIds, UUID assessmentId,
                                        EligibilityService eligibilityService,
                                        PeopleService peopleService)
    {
        Map<Long, String> registrations = peopleService.listStudents().stream()
                .collect(Collectors.toMap(
                        profile -> profile.personId(),
                        profile -> profile.registrationNumber(),
                        (first, second) -> first));

        List<CandidateRow> rows = new ArrayList<>();

        for (long personId : candidateIds)
        {
            EligibilityService.Eligibility eligibility =
                    eligibilityService.check(assessmentId, personId);
            String name = peopleService.findById(personId)
                    .map(person -> person.firstName() + " " + person.lastName())
                    .orElse("Unknown person");
            rows.add(new CandidateRow(personId, name,
                    registrations.getOrDefault(personId, ""),
                    eligibility.eligible(), eligibility.reasons(),
                    eligibility.eligible() ? ""
                            : EligibilityPresentation.reasonMessages(eligibility, personId)));
        }

        return rows;
    }

    private String courseLabel(Assessment assessment)
    {
        try
        {
            var course = courseService.get(assessment.courseId());
            return course.code() + " - " + course.title();
        }
        catch (CourseNotFoundException exception)
        {
            return "Unknown course";
        }
    }
}
