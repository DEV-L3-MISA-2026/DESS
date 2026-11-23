package com.dess.platform.web.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Overview page model: summary figures and the next active bookings.
 */
public final class DashboardView implements PageView
{
    private final PageChrome chrome;
    private final List<StatCard> stats;
    private final List<BookingRow> bookings;
    private final EmptyState bookingsEmpty;

    /**
     * @param admin whether the signed-in account may see the account figure
     * @param studentCount number of students
     * @param courseCount number of courses
     * @param periodCount number of academic periods
     * @param programCount number of programs
     * @param roomCount number of rooms
     * @param bookingCount number of bookings
     * @param accountCount number of sign-in accounts
     * @param bookings next active bookings
     */
    public DashboardView(boolean admin, int studentCount, int courseCount, int periodCount,
                         int programCount, int roomCount, int bookingCount, int accountCount,
                         List<BookingRow> bookings)
    {
        this.chrome = PageChrome.builder("Overview", "Academic operations", "overview",
                        "/WEB-INF/views/dashboard/index.jsp")
                .description("Current counts and the next active bookings. Each figure "
                        + "links to the list it summarises.")
                .build();

        List<StatCard> cards = new ArrayList<>();
        cards.add(new StatCard("Students", studentCount, "People directory", "/app/people"));
        cards.add(new StatCard("Courses", courseCount, "Course catalogue", "/app/academic"));
        cards.add(new StatCard("Periods", periodCount, "Academic calendar", "/app/academic"));
        cards.add(new StatCard("Programs", programCount, "Programmes and versions",
                "/app/academic"));
        cards.add(new StatCard("Rooms", roomCount, "Schedulable spaces", "/app/scheduling"));
        cards.add(new StatCard("Bookings", bookingCount, "Active and cancelled",
                "/app/scheduling"));

        if (admin)
        {
            cards.add(new StatCard("Accounts", accountCount, "Sign-in accounts",
                    "/app/security"));
        }

        this.stats = List.copyOf(cards);
        this.bookings = List.copyOf(bookings);
        this.bookingsEmpty = EmptyState.of("No upcoming booking.",
                "Active bookings that start from now on appear here once the scheduling "
                        + "service stores them.");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<StatCard> getStats()
    {
        return stats;
    }

    public List<BookingRow> getBookings()
    {
        return bookings;
    }

    public EmptyState getBookingsEmpty()
    {
        return bookingsEmpty;
    }
}
