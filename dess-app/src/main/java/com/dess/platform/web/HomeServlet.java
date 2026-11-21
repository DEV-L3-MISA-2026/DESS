package com.dess.platform.web;

import com.dess.academic.application.CourseService;
import com.dess.academic.application.PeriodService;
import com.dess.academic.application.ProgramService;
import com.dess.people.application.PeopleService;
import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.RoomService;
import com.dess.security.application.UserManagementService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Overview page.
 *
 * <p>The numbers come from the module services, not from placeholder data. Each card
 * links to the list it summarises.</p>
 */
@WebServlet({"/app", "/app/"})
public class HomeServlet extends HttpServlet
{
    private static final String VIEW = "/WEB-INF/views/dashboard/index.jsp";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    private PeopleService peopleService;

    @Inject
    private CourseService courseService;

    @Inject
    private PeriodService periodService;

    @Inject
    private ProgramService programService;

    @Inject
    private RoomService roomService;

    @Inject
    private BookingService bookingService;

    @Inject
    private UserManagementService userManagementService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);

        request.setAttribute("studentCount", peopleService.listStudents().size());
        request.setAttribute("courseCount", courseService.list().size());
        request.setAttribute("periodCount", periodService.listAll().size());
        request.setAttribute("programCount", programService.listAll().size());
        request.setAttribute("roomCount", roomService.findAll().size());

        var allBookings = bookingService.findAll();
        List<Map<String, Object>> upcoming = BookingPresentation
                .upcoming(allBookings, OffsetDateTime.now(ZoneOffset.UTC), 6)
                .stream()
                .map(booking -> Map.<String, Object>of(
                        "id", booking.id().toString(),
                        "ownerType", BookingPresentation.ownerLabel(booking.ownerType()),
                        "startsAt", DATE_TIME.format(booking.startsAt().withOffsetSameInstant(ZoneOffset.UTC)),
                        "endsAt", DATE_TIME.format(booking.endsAt().withOffsetSameInstant(ZoneOffset.UTC))))
                .toList();

        request.setAttribute("bookingCount", allBookings.size());
        request.setAttribute("upcomingBookings", upcoming);
        request.setAttribute("accountCount", userManagementService.findAll().size());
        request.setAttribute("activeNav", "overview");

        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
