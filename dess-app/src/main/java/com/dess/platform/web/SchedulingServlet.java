package com.dess.platform.web;

import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.RoomService;
import com.dess.scheduling.domain.Room;
import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.RoleCatalog;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rooms and bookings list.
 *
 * <p>Bookings are read-only here. Creating a session booking needs the course,
 * group and teacher context that the scheduling flow will provide; this page does not
 * pretend that a booking is confirmed before the service validates it.</p>
 */
@WebServlet("/app/scheduling")
public class SchedulingServlet extends HttpServlet
{
    private static final String VIEW = "/WEB-INF/views/scheduling/index.jsp";
    private static final String LIST_PATH = "/app/scheduling";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    private RoomService roomService;

    @Inject
    private BookingService bookingService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        request.setAttribute("roomForm", Web.form());
        request.setAttribute("roomErrors", Map.of());
        showPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");

        if (!Web.requirePermission(request, response, authorizationService,
                RoleCatalog.SCHEDULING_WRITE))
        {
            return;
        }

        String action = Web.param(request, "action");

        if (!"createRoom".equals(action))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
            return;
        }

        Map<String, String> form = Web.form();
        form.put("code", Web.param(request, "code"));
        form.put("name", Web.param(request, "name"));
        form.put("capacity", Web.param(request, "capacity"));
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            int capacity = Integer.parseInt(form.get("capacity"));
            Room room = roomService.create(form.get("code"), form.get("name"), capacity);
            Web.flash(request, "success", "Room " + room.code() + " was created.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (NumberFormatException exception)
        {
            errors.put("capacity", "Enter a whole number greater than zero.");
            renderError(request, response, form, errors);
        }
        catch (IllegalArgumentException exception)
        {
            if (form.get("code").isBlank())
            {
                errors.put("code", "Enter a room code.");
            }
            if (form.get("name").isBlank())
            {
                errors.put("name", "Enter a room name.");
            }
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, form, errors);
        }
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        List<Map<String, Object>> rooms = roomService.findAll().stream()
                .map(room -> Map.<String, Object>of(
                        "id", room.id().toString(),
                        "code", room.code(),
                        "name", room.name(),
                        "capacity", room.capacity()))
                .toList();

        List<Map<String, Object>> bookings = bookingService.findAll().stream()
                .map(booking -> Map.<String, Object>of(
                        "id", booking.id().toString(),
                        "ownerType", booking.ownerType(),
                        "ownerId", booking.ownerId().toString(),
                        "startsAt", DATE_TIME.format(booking.startsAt()),
                        "endsAt", DATE_TIME.format(booking.endsAt()),
                        "status", booking.status().name()))
                .toList();

        request.setAttribute("rooms", rooms);
        request.setAttribute("bookings", bookings);
        request.setAttribute("canWrite", canWrite(request));
        request.setAttribute("activeNav", "scheduling");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("roomForm", form);
        request.setAttribute("roomErrors", errors);
        showPage(request, response);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        var user = Web.currentUser(request);

        return user != null && authorizationService.isPermitted(user.toAccount(),
                RoleCatalog.SCHEDULING_WRITE);
    }
}
