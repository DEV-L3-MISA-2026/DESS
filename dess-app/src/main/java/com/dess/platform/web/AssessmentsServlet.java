package com.dess.platform.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.dess.academic.domain.Course;
import com.dess.assessment.application.AssessmentService;
import com.dess.assessment.application.CreateAssessmentCommand;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.AssessmentType;
import com.dess.assessment.exception.AssessmentException;
import com.dess.platform.web.view.AssessmentRow;
import com.dess.platform.web.view.AssessmentsView;
import com.dess.platform.web.view.Badge;
import com.dess.platform.web.view.Option;
import com.dess.platform.web.view.Views;
import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.RoleCatalog;

/**
 * @brief Assessment definitions page.
 *
 * <p>The page lists the assessment definitions with their type and status,
 * filtered by type and course through the URL, and offers the create form
 * to accounts that may write. Professors are read-only: the form is hidden
 * and every write is refused on the server with 403, following the same
 * {@code academic:write} check as the exam-sessions page.</p>
 *
 * <p>There is no period filter. An assessment has no period until issue #18
 * (course offerings) lands, and no period is invented here.</p>
 */
@WebServlet("/app/assessments")
public class AssessmentsServlet extends HttpServlet
{
    private static final String LIST_PATH = "/app/assessments";

    @Inject
    private AssessmentService assessmentService;

    @Inject
    private CourseService courseService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        showPage(request, response, Web.form(), Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");

        if (!Web.requirePermission(request, response, authorizationService,
                RoleCatalog.ACADEMIC_WRITE))
        {
            return;
        }

        String action = Web.param(request, "action");

        if (!"createAssessment".equals(action))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
            return;
        }

        createAssessment(request, response);
    }

    private void createAssessment(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = readCreateForm(request);
        Map<String, String> errors = new LinkedHashMap<>();
        Set<UUID> knownCourses = courseService.list().stream()
                .map(Course::id).collect(Collectors.toSet());

        ParsedAssessmentForm parsed = parseCreateForm(form, errors, knownCourses);

        if (!errors.isEmpty() || parsed == null)
        {
            renderError(request, response, form, errors);
            return;
        }

        try
        {
            Assessment created = assessmentService.create(new CreateAssessmentCommand(
                    parsed.courseId(), parsed.title(), parsed.type(),
                    parsed.maximumScore(), parsed.durationMinutes()));
            Web.flash(request, "success", "Assessment \"" + created.title() + "\" was created.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (AssessmentException | IllegalArgumentException exception)
        {
            errors.put("form", exception.getMessage());
            renderError(request, response, form, errors);
        }
    }

    private Map<String, String> readCreateForm(HttpServletRequest request)
    {
        Map<String, String> form = Web.form();
        form.put("courseId", Web.param(request, "courseId"));
        form.put("title", Web.param(request, "title"));
        form.put("type", Web.param(request, "type"));
        form.put("maximumScore", Web.param(request, "maximumScore"));
        form.put("durationMinutes", Web.param(request, "durationMinutes"));
        return form;
    }

    /**
     * @brief Validates the submitted create values.
     *
     * <p>Every failure is recorded in {@code errors} under the DOM id of the
     * field, so the validation summary links to it and the entered values
     * stay in the form.</p>
     *
     * @param form submitted values
     * @param errors map that receives one message per invalid field
     * @param knownCourseIds ids of the courses the form may reference
     * @return the parsed values, or {@code null} when any field is invalid
     */
    static ParsedAssessmentForm parseCreateForm(Map<String, String> form,
                                                Map<String, String> errors,
                                                Set<UUID> knownCourseIds)
    {
        UUID courseId = null;
        String courseValue = form.getOrDefault("courseId", "");

        if (courseValue.isBlank())
        {
            errors.put("courseId", "Choose a course.");
        }
        else
        {
            try
            {
                courseId = UUID.fromString(courseValue);
                if (!knownCourseIds.contains(courseId))
                {
                    errors.put("courseId", "Choose a known course.");
                    courseId = null;
                }
            }
            catch (IllegalArgumentException exception)
            {
                errors.put("courseId", "Choose a valid course.");
            }
        }

        String title = form.getOrDefault("title", "");

        if (title.isBlank())
        {
            errors.put("title", "Enter a title.");
        }
        else if (title.length() > 200)
        {
            errors.put("title", "The title must contain at most 200 characters.");
        }

        AssessmentType type = null;
        String typeValue = form.getOrDefault("type", "");

        if (typeValue.isBlank())
        {
            errors.put("type", "Choose a type.");
        }
        else
        {
            try
            {
                type = AssessmentType.valueOf(typeValue);
            }
            catch (IllegalArgumentException exception)
            {
                errors.put("type", "Choose a valid type.");
            }
        }

        BigDecimal maximumScore = null;
        String maximumValue = form.getOrDefault("maximumScore", "");

        if (maximumValue.isBlank())
        {
            errors.put("maximumScore", "Enter a maximum score.");
        }
        else
        {
            try
            {
                maximumScore = new BigDecimal(maximumValue);
                if (maximumScore.compareTo(BigDecimal.ZERO) <= 0)
                {
                    errors.put("maximumScore", "The maximum score must be greater than zero.");
                    maximumScore = null;
                }
                else if (maximumScore.scale() > 2)
                {
                    errors.put("maximumScore",
                            "The maximum score must have at most two decimal places.");
                    maximumScore = null;
                }
                else if (maximumScore.compareTo(new BigDecimal("9999.99")) > 0)
                {
                    errors.put("maximumScore", "The maximum score must be at most 9999.99.");
                    maximumScore = null;
                }
            }
            catch (NumberFormatException exception)
            {
                errors.put("maximumScore", "Enter a number for the maximum score.");
            }
        }

        Integer durationMinutes = null;
        String durationValue = form.getOrDefault("durationMinutes", "");

        if (durationValue.isBlank())
        {
            errors.put("durationMinutes", "Enter a duration in minutes.");
        }
        else
        {
            try
            {
                durationMinutes = Integer.valueOf(durationValue);
                if (durationMinutes <= 0)
                {
                    errors.put("durationMinutes", "The duration must be greater than zero.");
                    durationMinutes = null;
                }
            }
            catch (NumberFormatException exception)
            {
                errors.put("durationMinutes", "Enter a whole number of minutes.");
            }
        }

        if (!errors.isEmpty())
        {
            return null;
        }

        return new ParsedAssessmentForm(courseId, title, type, maximumScore, durationMinutes);
    }

    /** @brief The validated values of the create form. */
    record ParsedAssessmentForm(UUID courseId, String title, AssessmentType type,
                                BigDecimal maximumScore, Integer durationMinutes)
    {
    }

    /**
     * @brief Applies the URL filters to the assessment list.
     *
     * <p>An unknown type or an unparseable course id matches nothing, so a
     * bad filter value shows the filtered empty state instead of silently
     * ignoring the filter.</p>
     *
     * @param assessments every assessment definition
     * @param typeFilter type name, or an empty string for no type filter
     * @param courseFilter course id, or an empty string for no course filter
     * @return the assessments that match both filters
     */
    static List<Assessment> filterAssessments(List<Assessment> assessments, String typeFilter,
                                              String courseFilter)
    {
        AssessmentType type = null;

        if (!typeFilter.isBlank())
        {
            try
            {
                type = AssessmentType.valueOf(typeFilter);
            }
            catch (IllegalArgumentException exception)
            {
                return List.of();
            }
        }

        UUID courseId = null;

        if (!courseFilter.isBlank())
        {
            try
            {
                courseId = UUID.fromString(courseFilter);
            }
            catch (IllegalArgumentException exception)
            {
                return List.of();
            }
        }

        List<Assessment> visible = new ArrayList<>();

        for (Assessment assessment : assessments)
        {
            if (type != null && assessment.type() != type)
            {
                continue;
            }
            if (courseId != null && !assessment.courseId().equals(courseId))
            {
                continue;
            }
            visible.add(assessment);
        }

        return visible;
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response,
                          Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        String typeFilter = Web.param(request, "type");
        String courseFilter = Web.param(request, "course");

        Map<UUID, String> courseLabels = courseService.list().stream()
                .collect(Collectors.toMap(Course::id,
                        course -> course.code() + " - " + course.title()));

        List<Option> typeOptions = new ArrayList<>();
        typeOptions.add(Option.of("", "All types", typeFilter.isBlank()));
        for (AssessmentType type : AssessmentType.values())
        {
            typeOptions.add(Option.of(type.name(), AssessmentPresentation.typeLabel(type),
                    type.name().equals(typeFilter)));
        }

        List<Option> courseOptions = new ArrayList<>();
        courseOptions.add(Option.of("", "All courses", courseFilter.isBlank()));
        courseLabels.forEach((id, label) -> courseOptions.add(
                Option.of(id.toString(), label, id.toString().equals(courseFilter))));

        List<Option> createCourses = courseLabels.entrySet().stream()
                .map(entry -> Option.of(entry.getKey().toString(), entry.getValue(),
                        entry.getKey().toString().equals(form.get("courseId"))))
                .toList();

        List<Option> createTypes = new ArrayList<>();
        for (AssessmentType type : AssessmentType.values())
        {
            createTypes.add(Option.of(type.name(), AssessmentPresentation.typeLabel(type),
                    type.name().equals(form.get("type"))));
        }

        List<AssessmentRow> rows = filterAssessments(assessmentService.list(),
                typeFilter, courseFilter).stream()
                .map(assessment -> new AssessmentRow(
                        assessment.id().toString(),
                        assessment.title(),
                        AssessmentPresentation.typeLabel(assessment.type()),
                        courseLabels.getOrDefault(assessment.courseId(), "Unknown course"),
                        assessment.maximumScore().toPlainString(),
                        assessment.durationMinutes(),
                        Badge.active(assessment.active())))
                .toList();

        AssessmentsView view = new AssessmentsView(rows, typeOptions, courseOptions,
                createCourses, createTypes, typeFilter, courseFilter, canWrite(request),
                form, errors);

        Views.render(request, response, view);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showPage(request, response, form, errors);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        var user = Web.currentUser(request);

        return user != null && authorizationService.isPermitted(user.toAccount(),
                RoleCatalog.ACADEMIC_WRITE);
    }
}
