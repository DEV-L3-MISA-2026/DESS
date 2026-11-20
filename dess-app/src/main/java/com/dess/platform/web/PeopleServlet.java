package com.dess.platform.web;

import com.dess.people.application.CreateStudentCommand;
import com.dess.people.application.PeopleService;
import com.dess.people.domain.Person;
import com.dess.people.domain.StudentProfile;
import com.dess.people.exception.DuplicateEmailException;
import com.dess.people.exception.DuplicateRegistrationNumberException;
import com.dess.people.exception.PeopleException;
import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.RoleCatalog;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Student list and creation form.
 *
 * <p>The table distinguishes the person record from a login account: a student does not
 * get an account here.</p>
 */
@WebServlet("/app/people")
public class PeopleServlet extends HttpServlet
{
    private static final String VIEW = "/WEB-INF/views/people/index.jsp";
    private static final String LIST_PATH = "/app/people";

    @Inject
    private PeopleService peopleService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        request.setAttribute("form", Web.form());
        request.setAttribute("errors", Map.of());
        showPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");

        if (!Web.requirePermission(request, response, authorizationService,
                RoleCatalog.PEOPLE_WRITE))
        {
            return;
        }

        Map<String, String> form = Web.form();
        form.put("firstName", Web.param(request, "firstName"));
        form.put("lastName", Web.param(request, "lastName"));
        form.put("email", Web.param(request, "email"));
        form.put("registrationNumber", Web.param(request, "registrationNumber"));

        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            CreateStudentCommand command = new CreateStudentCommand(
                    form.get("email"),
                    form.get("lastName"),
                    form.get("firstName"),
                    form.get("registrationNumber"));

            StudentProfile profile = peopleService.createStudent(command);

            Web.flash(request, "success",
                    "Student " + form.get("firstName") + " " + form.get("lastName")
                            + " was created with registration number "
                            + profile.registrationNumber() + ".");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (DuplicateEmailException exception)
        {
            errors.put("email", "This email address is already used by another person.");
            renderWithErrors(request, response, form, errors);
        }
        catch (DuplicateRegistrationNumberException exception)
        {
            errors.put("registrationNumber",
                    "This registration number is already used by another student.");
            renderWithErrors(request, response, form, errors);
        }
        catch (IllegalArgumentException exception)
        {
            addRequiredErrors(form, errors);
            errors.putIfAbsent("form", exception.getMessage());
            renderWithErrors(request, response, form, errors);
        }
        catch (PeopleException exception)
        {
            errors.put("form", exception.getMessage());
            renderWithErrors(request, response, form, errors);
        }
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String query = Web.param(request, "q").toLowerCase();
        List<Map<String, Object>> rows = new ArrayList<>();

        for (StudentProfile student : peopleService.listStudents())
        {
            Optional<Person> person = peopleService.findById(student.personId());

            if (person.isEmpty())
            {
                continue;
            }

            Person value = person.get();
            String fullName = (value.firstName() + " " + value.lastName()).trim();
            String haystack = (fullName + " " + value.email() + " "
                    + student.registrationNumber()).toLowerCase();

            if (!query.isEmpty() && !haystack.contains(query))
            {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("personId", value.id());
            row.put("name", fullName);
            row.put("email", value.email());
            row.put("registrationNumber", student.registrationNumber());
            row.put("active", value.active());
            rows.add(row);
        }

        request.setAttribute("students", rows);
        request.setAttribute("q", Web.param(request, "q"));
        request.setAttribute("canWrite", canWrite(request));
        request.setAttribute("activeNav", "people");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void renderWithErrors(HttpServletRequest request, HttpServletResponse response,
                                  Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        showPage(request, response);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        var user = Web.currentUser(request);

        return user != null && authorizationService.isPermitted(user.toAccount(),
                RoleCatalog.PEOPLE_WRITE);
    }

    private static void addRequiredErrors(Map<String, String> form, Map<String, String> errors)
    {
        if (form.get("firstName").isBlank())
        {
            errors.put("firstName", "Enter the first name.");
        }
        if (form.get("lastName").isBlank())
        {
            errors.put("lastName", "Enter the last name.");
        }
        if (form.get("email").isBlank())
        {
            errors.put("email", "Enter the email address.");
        }
        if (form.get("registrationNumber").isBlank())
        {
            errors.put("registrationNumber", "Enter the registration number.");
        }
    }
}
