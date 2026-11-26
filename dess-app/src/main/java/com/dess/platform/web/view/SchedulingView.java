package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;

/** Rooms and bookings on the scheduling page. */
public final class SchedulingView implements PageView
{
    private final PageChrome chrome;
    private final List<RoomRow> rooms;
    private final List<BookingRow> bookings;
    private final Map<String, String> roomForm;
    private final Map<String, String> roomErrors;
    private final String roomCountLabel;
    private final EmptyState roomsEmpty;
    private final EmptyState bookingsEmpty;

    /**
     * @param rooms rooms
     * @param bookings bookings, newest first
     * @param canWrite whether the account may create a room
     * @param roomForm submitted room values
     * @param roomErrors room field errors
     */
    public SchedulingView(List<RoomRow> rooms, List<BookingRow> bookings, boolean canWrite,
                          Map<String, String> roomForm, Map<String, String> roomErrors)
    {
        this.chrome = PageChrome.builder("Scheduling", "Rooms and bookings", "scheduling",
                        "/WEB-INF/views/scheduling/index.jsp")
                .description("Rooms and the bookings that reserve them. A booking appears "
                        + "as active only after the scheduling service has stored it. The "
                        + "weekly timetable is not available yet.")
                .breadcrumb("Overview", "/app")
                .writable(canWrite)
                .build();
        this.rooms = List.copyOf(rooms);
        this.bookings = List.copyOf(bookings);
        this.roomForm = Map.copyOf(roomForm);
        this.roomErrors = Map.copyOf(roomErrors);
        this.roomCountLabel = Views.count(rooms.size(), "room", "rooms") + ".";
        this.roomsEmpty = EmptyState.of("No room yet.",
                "Add a room before a teaching session or an examination can use it.");
        this.bookingsEmpty = EmptyState.of("No booking yet.",
                "Teaching sessions and examinations will appear here once they are scheduled.");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<RoomRow> getRooms()
    {
        return rooms;
    }

    public List<BookingRow> getBookings()
    {
        return bookings;
    }

    public Map<String, String> getRoomForm()
    {
        return roomForm;
    }

    public Map<String, String> getRoomErrors()
    {
        return roomErrors;
    }

    public String getRoomCountLabel()
    {
        return roomCountLabel;
    }

    public EmptyState getRoomsEmpty()
    {
        return roomsEmpty;
    }

    public EmptyState getBookingsEmpty()
    {
        return bookingsEmpty;
    }
}
