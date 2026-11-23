package com.dess.platform.web;

import com.dess.academic.application.CourseService;
import com.dess.academic.application.PeriodService;
import com.dess.academic.application.ProgramService;
import com.dess.people.application.PeopleService;
import com.dess.platform.web.view.BookingRow;
import com.dess.platform.web.view.DashboardView;
import com.dess.platform.web.view.Views;
import com.dess.scheduling.application.BookingService;
import com.dess.scheduling.application.RoomService;
import com.dess.scheduling.domain.BookingStatus;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Overview page.
 *
 * <p>The numbers come from the module services, not from placeholder data. Each card
 * links to the list it summarises.</p>
 */
@WebServlet({"/app", "/app/"})
public class HomeServlet extends HttpServlet
{
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

        var allBookings = bookingService.findAll();
        Map<UUID, String> roomCodes = roomService.findAll().stream()
                .collect(Collectors.toMap(room -> room.id(), room -> room.code()));
        List<BookingRow> upcoming = BookingPresentation
                .upcoming(allBookings, OffsetDateTime.now(ZoneOffset.UTC), 6)
                .stream()
                .map(booking -> new BookingRow(
                        BookingPresentation.ownerLabel(booking.ownerType()),
                        BookingPresentation.resources(
                                bookingService.findSlotsByBookingId(booking.id()), roomCodes),
                        DATE_TIME.format(booking.startsAt()
                                .withOffsetSameInstant(ZoneOffset.UTC)),
                        DATE_TIME.format(booking.endsAt()
                                .withOffsetSameInstant(ZoneOffset.UTC)),
                        booking.status() == BookingStatus.ACTIVE))
                .toList();

        var user = Web.currentUser(request);

        DashboardView view = new DashboardView(
                user != null && user.isAdmin(),
                peopleService.listStudents().size(),
                courseService.list().size(),
                periodService.listAll().size(),
                programService.listAll().size(),
                roomService.findAll().size(),
                allBookings.size(),
                userManagementService.findAll().size(),
                upcoming);

        Views.render(request, response, view);
    }
}
