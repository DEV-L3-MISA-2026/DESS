package com.dess.platform.web;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.dess.assessment.application.AssessmentService;
import com.dess.assessment.application.ExamSessionService;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.domain.ExamSessionConflictException;
import com.dess.people.application.PeopleService;
import com.dess.platform.web.view.ExamSessionsView;
import com.dess.platform.web.view.ManagedSessionRow;
import com.dess.platform.web.view.Option;
import com.dess.platform.web.view.Views;
import com.dess.scheduling.application.RoomService;
import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.RoleCatalog;

/**
 * @brief Management page for exam sessions.
 *
 * <p>Creation, search by session id, invigilator assignment and cancellation live here so
 * the read-only examinations page stays small. A session is shown only after the server
 * stored it and the booking service confirmed its resources.</p>
 */
@WebServlet("/app/exam-sessions")
public class ExamSessionsServlet extends HttpServlet
{
    private static final String LIST_PATH = "/app/exam-sessions";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    private ExamSessionService examSessionService;

    @Inject
    private AssessmentService assessmentService;

    @Inject
    private RoomService roomService;

    @Inject
    private PeopleService peopleService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        showPage(request, response, Web.form(), Map.of(), Web.form(), Map.of(), Map.of());
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
            case "createSession" -> createSession(request, response);
            case "assignInvigilators" -> assignInvigilators(request, response);
            case "cancelSession" -> cancelSession(request, response);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
        }
    }

    private void createSession(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = readSessionForm(request);
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            UUID assessmentId = parseUuid(form.get("assessmentId"), "assessment", errors,
                    "assessmentId");
            UUID roomId = parseUuid(form.get("roomId"), "room", errors, "roomId");
            OffsetDateTime startsAt = parseDateTime(form.get("startsAt"), "start", errors,
                    "startsAt");
            OffsetDateTime endsAt = parseDateTime(form.get("endsAt"), "end", errors, "endsAt");
            List<Long> invigilators = parseInvigilators(form.get("invigilators"), errors,
                    "invigilators");

            if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt))
            {
                errors.put("endsAt", "The end must be after the start.");
            }

            if (!errors.isEmpty())
            {
                renderError(request, response, form, errors, Web.form(), Map.of(),
                        Map.of());
                return;
            }

            ExamSession session = examSessionService.create(assessmentId, roomId,
                    startsAt, endsAt, invigilators);
            Web.flash(request, "success", "Exam session " + session.id() + " was scheduled.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (ExamSessionConflictException exception)
        {
            errors.put("form", exception.getMessage());
            renderError(request, response, form, errors, Web.form(), Map.of(),
                    Map.of());
        }
        catch (IllegalArgumentException exception)
        {
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, form, errors, Web.form(), Map.of(),
                    Map.of());
        }
    }

    private void assignInvigilators(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = Web.form();
        form.put("sessionId", Web.param(request, "sessionId"));
        form.put("invigilators", Web.param(request, "invigilators"));
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            UUID sessionId = parseUuid(form.get("sessionId"), "session", errors, "sessionId");
            List<Long> invigilators = parseInvigilators(form.get("invigilators"), errors,
                    "invigilators");

            if (!errors.isEmpty())
            {
                renderError(request, response, Web.form(), Map.of(), form, errors,
                        Map.of());
                return;
            }

            examSessionService.withInvigilators(sessionId, invigilators);
            Web.flash(request, "success", "Invigilators were updated.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (ExamSessionConflictException exception)
        {
            errors.put("form", exception.getMessage());
            renderError(request, response, Web.form(), Map.of(), form, errors,
                    Map.of());
        }
        catch (IllegalArgumentException exception)
        {
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, Web.form(), Map.of(), form, errors,
                    Map.of());
        }
    }

    private void cancelSession(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = Web.form();
        form.put("sessionId", Web.param(request, "sessionId"));
        Map<String, String> errors = new LinkedHashMap<>();

        UUID sessionId = parseUuid(form.get("sessionId"), "session", errors, "sessionId");

        if (!errors.isEmpty())
        {
            renderError(request, response, Web.form(), Map.of(), Web.form(), Map.of(),
                    errors);
            return;
        }

        try
        {
            examSessionService.cancel(sessionId);
            Web.flash(request, "success", "Exam session " + sessionId + " was cancelled.");
        }
        catch (IllegalArgumentException exception)
        {
            Web.flash(request, "error", exception.getMessage());
        }

        response.sendRedirect(request.getContextPath() + LIST_PATH);
    }

    private Map<String, String> readSessionForm(HttpServletRequest request)
    {
        Map<String, String> form = Web.form();
        form.put("assessmentId", Web.param(request, "assessmentId"));
        form.put("roomId", Web.param(request, "roomId"));
        form.put("startsAt", Web.param(request, "startsAt"));
        form.put("endsAt", Web.param(request, "endsAt"));
        form.put("invigilators", Web.param(request, "invigilators"));
        return form;
    }

    private UUID parseUuid(String value, String label, Map<String, String> errors, String field)
    {
        if (value == null || value.isBlank())
        {
            errors.put(field, "Enter the " + label + " id.");
            return null;
        }
        try
        {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException exception)
        {
            errors.put(field, "Enter a valid " + label + " id.");
            return null;
        }
    }

    private OffsetDateTime parseDateTime(String value, String label,
                                         Map<String, String> errors, String field)
    {
        if (value == null || value.isBlank())
        {
            errors.put(field, "Enter the " + label + " date and time.");
            return null;
        }
        try
        {
            return LocalDateTime.parse(value).atOffset(ZoneOffset.UTC);
        }
        catch (DateTimeParseException exception)
        {
            errors.put(field, "Use the format yyyy-MM-ddTHH:mm.");
            return null;
        }
    }

    private List<Long> parseInvigilators(String value, Map<String, String> errors, String field)
    {
        List<Long> ids = new ArrayList<>();

        if (value == null || value.isBlank())
        {
            return ids;
        }

        for (String part : value.split(","))
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
                if (peopleService.findById(personId).isEmpty())
                {
                    errors.put(field, "No person exists with id " + personId + ".");
                    continue;
                }
                if (ids.contains(personId))
                {
                    errors.put(field, "A person cannot be twice in the same session.");
                    continue;
                }
                ids.add(personId);
            }
            catch (NumberFormatException exception)
            {
                errors.put(field, "Enter person ids as whole numbers separated by commas.");
            }
        }

        return ids;
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response,
                          Map<String, String> sessionForm, Map<String, String> sessionErrors,
                          Map<String, String> invigilatorForm,
                          Map<String, String> invigilatorErrors,
                          Map<String, String> cancelErrors)
            throws ServletException, IOException
    {
        var allRooms = roomService.findAll();
        Map<UUID, String> roomLabels = allRooms.stream()
                .collect(Collectors.toMap(room -> room.id(),
                        room -> room.code() + " - " + room.name()));

        List<Option> rooms = allRooms.stream()
                .map(room -> Option.of(room.id().toString(),
                        room.code() + " - " + room.name(),
                        room.id().toString().equals(sessionForm.get("roomId"))))
                .toList();

        Map<UUID, String> assessmentLabels =
                AssessmentPresentation.labels(assessmentService.list());
        List<Option> assessments = assessmentLabels.entrySet().stream()
                .map(entry -> Option.of(entry.getKey().toString(), entry.getValue()))
                .toList();

        List<ManagedSessionRow> allSessions = examSessionService.findAll().stream()
                .map(session -> new ManagedSessionRow(
                        session.id().toString(),
                        AssessmentPresentation.label(session.assessmentId(),
                                assessmentLabels),
                        DATE_TIME.format(session.startsAt()),
                        DATE_TIME.format(session.endsAt()),
                        roomLabels.getOrDefault(session.roomId(), "Unknown room"),
                        session.candidatePersonIds().size(),
                        String.join(", ", session.invigilatorPersonIds()
                                .stream().map(String::valueOf).toList())))
                .toList();

        String query = Web.param(request, "q");
        List<ManagedSessionRow> sessions = allSessions.stream()
                .filter(session -> query.isBlank()
                        || session.getId().toLowerCase().contains(query.toLowerCase()))
                .toList();

        List<Option> selectableSessions = allSessions.stream()
                .map(session -> Option.of(session.getId(),
                        session.getStartsAt() + " - " + session.getRoom(),
                        session.getId().equals(invigilatorForm.get("sessionId"))))
                .toList();

        ExamSessionsView view = new ExamSessionsView(sessions, selectableSessions,
                assessments, rooms, query, canWrite(request), sessionForm, sessionErrors,
                invigilatorForm, invigilatorErrors, cancelErrors);

        Views.render(request, response, view);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             Map<String, String> sessionForm, Map<String, String> sessionErrors,
                             Map<String, String> invigilatorForm,
                             Map<String, String> invigilatorErrors,
                             Map<String, String> cancelErrors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showPage(request, response, sessionForm, sessionErrors,
                invigilatorForm, invigilatorErrors, cancelErrors);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        var user = Web.currentUser(request);

        return user != null && authorizationService.isPermitted(user.toAccount(),
                RoleCatalog.ACADEMIC_WRITE);
    }
}
