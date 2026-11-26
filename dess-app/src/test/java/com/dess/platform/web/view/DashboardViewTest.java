package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardViewTest
{
    @Test
    void anAdministratorSeesTheAccountFigure()
    {
        DashboardView view = view(true);

        assertEquals(7, view.getStats().size());
        assertEquals("Accounts", view.getStats().get(6).getLabel());
    }

    @Test
    void aProfessorDoesNotSeeTheAccountFigure()
    {
        DashboardView view = view(false);

        assertEquals(6, view.getStats().size());
    }

    @Test
    void theEmptyBookingsStateDoesNotPromiseAFutureBooking()
    {
        DashboardView view = view(false);

        assertEquals("No upcoming booking.", view.getBookingsEmpty().getTitle());
        assertEquals("", view.getBookingsEmpty().getActionLabel());
    }

    private static DashboardView view(boolean admin)
    {
        return new DashboardView(admin, 1, 2, 3, 4, 5, 6, 7, List.of());
    }
}
