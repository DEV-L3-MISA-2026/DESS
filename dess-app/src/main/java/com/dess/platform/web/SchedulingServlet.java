package com.dess.platform.web;

import com.dess.platform.web.view.BookingRow;
import com.dess.platform.web.view.RoomRow;
import com.dess.platform.web.view.SchedulingView;
import com.dess.platform.web.view.Views;
import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.RoomService;
import com.dess.scheduling.domain.BookingStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final String LIST_PATH = "/app/scheduling";
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
        render(request, response, Web.form(), Map.of());
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

        if (form.get("code").isBlank())
        {
            errors.put("roomCode", "Enter a room code.");
        }
        if (form.get("name").isBlank())
        {
            errors.put("roomName", "Enter a room name.");
        }

        int capacity = 0;

        try
        {
            capacity = Integer.parseInt(form.get("capacity"));
            if (capacity < 1)
            {
                errors.put("roomCapacity", "Enter a whole number greater than zero.");
            }
        }
        catch (NumberFormatException exception)
        {
            errors.put("roomCapacity", "Enter a whole number greater than zero.");
        }

        if (!errors.isEmpty())
        {
            renderError(request, response, form, errors);
            return;
        }

        try
        {
            Room room = roomService.create(form.get("code"), form.get("name"), capacity);
            Web.flash(request, "success", "Room " + room.code() + " was created.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (IllegalArgumentException exception)
        {
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, form, errors);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response,
                        Map<String, String> roomForm, Map<String, String> roomErrors)
            throws ServletException, IOException
    {
        var allRooms = roomService.findAll();

        List<RoomRow> rooms = allRooms.stream()
                .map(room -> new RoomRow(room.code(), room.name(), room.capacity()))
                .toList();

        Map<UUID, String> roomCodes = allRooms.stream()
                .collect(Collectors.toMap(Room::id, Room::code));

        // The service has no bulk slot query yet. The list is small, so one
        // lookup per booking keeps the resource labels accurate for now.
        List<BookingRow> bookings = bookingService.findAll().stream()
                .map(booking -> new BookingRow(
                        BookingPresentation.ownerLabel(booking.ownerType()),
                        BookingPresentation.resources(
                                bookingService.findSlotsByBookingId(booking.id()), roomCodes),
                        Web.moment(booking.startsAt()),
                        Web.moment(booking.endsAt()),
                        booking.status() == BookingStatus.ACTIVE))
                .toList();

        SchedulingView view = new SchedulingView(rooms, bookings, canWrite(request),
                roomForm, roomErrors);

        Views.render(request, response, view);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        render(request, response, form, errors);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        return Web.canWrite(request, authorizationService, RoleCatalog.SCHEDULING_WRITE);
    }
}
