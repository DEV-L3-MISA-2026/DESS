package com.dess.platform.web;

import com.dess.academic.application.CourseService;
import com.dess.academic.application.CreateCourseCommand;
import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.PeriodService;
import com.dess.academic.application.ProgramService;
import com.dess.academic.domain.AcademicProgramStatus;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.DuplicateCourseCodeException;
import com.dess.academic.exception.DuplicatePeriodException;
import com.dess.platform.web.view.AcademicView;
import com.dess.platform.web.view.CourseRow;
import com.dess.platform.web.view.PeriodRow;
import com.dess.platform.web.view.ProgramRow;
import com.dess.platform.web.view.Views;
import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.RoleCatalog;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Academic catalog page: periods, programs and courses.
 *
 * <p>The page separates the three concepts and keeps each creation form next to its
 * list. A course is the catalogue entry; offerings are not implemented yet.</p>
 */
@WebServlet("/app/academic")
public class AcademicServlet extends HttpServlet
{
    private static final String LIST_PATH = "/app/academic";

    @Inject
    private PeriodService periodService;

    @Inject
    private ProgramService programService;

    @Inject
    private CourseService courseService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        render(request, response,
                Web.form(), Map.of(),
                Web.form(), Map.of(),
                Web.form(), Map.of());
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

        switch (action)
        {
            case "createCourse" -> createCourse(request, response);
            case "createPeriod" -> createPeriod(request, response);
            case "createProgram" -> createProgram(request, response);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Unknown action.");
        }
    }

    private void createCourse(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = Web.form();
        form.put("code", Web.param(request, "code"));
        form.put("title", Web.param(request, "title"));
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            courseService.create(new CreateCourseCommand(form.get("code"), form.get("title")));
            Web.flash(request, "success", "Course " + form.get("code").toUpperCase()
                    + " was added to the catalogue.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (DuplicateCourseCodeException exception)
        {
            errors.put("courseCode", "This course code is already in use.");
            renderError(request, response, "course", form, errors);
        }
        catch (IllegalArgumentException exception)
        {
            if (form.get("code").isBlank())
            {
                errors.put("courseCode", "Enter a course code.");
            }
            if (form.get("title").isBlank())
            {
                errors.put("courseTitle", "Enter a course title.");
            }
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, "course", form, errors);
        }
    }

    private void createPeriod(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = Web.form();
        form.put("academicYear", Web.param(request, "academicYear"));
        form.put("term", Web.param(request, "term"));
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            int term = Integer.parseInt(form.get("term"));
            periodService.create(new CreatePeriodCommand(form.get("academicYear"), term));
            Web.flash(request, "success", "Academic period " + form.get("academicYear")
                    + " term " + term + " was created.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (NumberFormatException exception)
        {
            errors.put("term", "Choose a term between 1 and 3.");
            renderError(request, response, "period", form, errors);
        }
        catch (DuplicatePeriodException exception)
        {
            errors.put("academicYear", "This period already exists.");
            renderError(request, response, "period", form, errors);
        }
        catch (IllegalArgumentException exception)
        {
            errors.put("form", exception.getMessage());
            renderError(request, response, "period", form, errors);
        }
    }

    private void createProgram(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = Web.form();
        form.put("code", Web.param(request, "code"));
        form.put("name", Web.param(request, "name"));
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            programService.create(new CreateProgramCommand(form.get("code"), form.get("name")));
            Web.flash(request, "success", "Program " + form.get("code").toUpperCase()
                    + " was created.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (AcademicException | IllegalArgumentException exception)
        {
            if (form.get("code").isBlank())
            {
                errors.put("programCode", "Enter a program code.");
            }
            if (form.get("name").isBlank())
            {
                errors.put("programName", "Enter a program name.");
            }
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, "program", form, errors);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response,
                        Map<String, String> periodForm, Map<String, String> periodErrors,
                        Map<String, String> programForm, Map<String, String> programErrors,
                        Map<String, String> courseForm, Map<String, String> courseErrors)
            throws ServletException, IOException
    {
        List<PeriodRow> periods = periodService.listAll().stream()
                .map(period -> new PeriodRow(period.academicYear(), period.term()))
                .toList();

        List<ProgramRow> programs = programService.listAll().stream()
                .map(program -> new ProgramRow(program.code(), program.name(),
                        program.status() == AcademicProgramStatus.ACTIVE))
                .toList();

        List<CourseRow> courses = courseService.list().stream()
                .map(course -> new CourseRow(course.code(), course.title(), course.active()))
                .toList();

        AcademicView view = new AcademicView(periods, programs, courses,
                Web.canWrite(request, authorizationService, RoleCatalog.ACADEMIC_WRITE),
                periodForm, periodErrors,
                programForm, programErrors,
                courseForm, courseErrors);

        Views.render(request, response, view);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             String formName, Map<String, String> form,
                             Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Map<String, String> empty = Map.of();
        Map<String, String> periodForm = "period".equals(formName) ? form : Web.form();
        Map<String, String> periodErrors = "period".equals(formName) ? errors : empty;
        Map<String, String> programForm = "program".equals(formName) ? form : Web.form();
        Map<String, String> programErrors = "program".equals(formName) ? errors : empty;
        Map<String, String> courseForm = "course".equals(formName) ? form : Web.form();
        Map<String, String> courseErrors = "course".equals(formName) ? errors : empty;

        render(request, response,
                periodForm, periodErrors,
                programForm, programErrors,
                courseForm, courseErrors);
    }
}
