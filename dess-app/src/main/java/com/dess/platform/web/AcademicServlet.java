package com.dess.platform.web;

import com.dess.academic.application.CourseService;
import com.dess.academic.application.CreateCourseCommand;
import com.dess.academic.application.CreatePeriodCommand;
import com.dess.academic.application.CreateProgramCommand;
import com.dess.academic.application.PeriodService;
import com.dess.academic.application.ProgramService;
import com.dess.academic.exception.AcademicException;
import com.dess.academic.exception.DuplicateCourseCodeException;
import com.dess.academic.exception.DuplicatePeriodException;
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
    private static final String VIEW = "/WEB-INF/views/academic/index.jsp";
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
        request.setAttribute("courseForm", Web.form());
        request.setAttribute("periodForm", Web.form());
        request.setAttribute("programForm", Web.form());
        request.setAttribute("courseErrors", Map.of());
        request.setAttribute("periodErrors", Map.of());
        request.setAttribute("programErrors", Map.of());
        showPage(request, response);
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

    private void showPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        List<Map<String, Object>> periods = periodService.listAll().stream()
                .map(period -> Map.<String, Object>of(
                        "id", period.id(),
                        "academicYear", period.academicYear(),
                        "term", period.term()))
                .toList();

        List<Map<String, Object>> programs = programService.listAll().stream()
                .map(program -> Map.<String, Object>of(
                        "id", program.id(),
                        "code", program.code(),
                        "name", program.name(),
                        "status", program.status().name()))
                .toList();

        List<Map<String, Object>> courses = courseService.list().stream()
                .map(course -> Map.<String, Object>of(
                        "id", course.id().toString(),
                        "code", course.code(),
                        "title", course.title(),
                        "active", course.active()))
                .toList();

        request.setAttribute("periods", periods);
        request.setAttribute("programs", programs);
        request.setAttribute("courses", courses);
        request.setAttribute("canWrite", canWrite(request));
        request.setAttribute("activeNav", "academic");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             String formName, Map<String, String> form,
                             Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("courseForm", Web.form());
        request.setAttribute("periodForm", Web.form());
        request.setAttribute("programForm", Web.form());
        request.setAttribute("courseErrors", Map.of());
        request.setAttribute("periodErrors", Map.of());
        request.setAttribute("programErrors", Map.of());

        request.setAttribute(formName + "Form", form);
        request.setAttribute(formName + "Errors", errors);
        showPage(request, response);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        var user = Web.currentUser(request);

        return user != null && authorizationService.isPermitted(user.toAccount(),
                RoleCatalog.ACADEMIC_WRITE);
    }
}
