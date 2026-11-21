package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BadgeTest
{
    @Test
    void activeUsesTheOkToneForAnActiveRecord()
    {
        Badge badge = Badge.active(true);

        assertEquals("ok", badge.getTone());
        assertEquals("Active", badge.getLabel());
    }

    @Test
    void activeUsesTheOffToneForAnInactiveRecord()
    {
        Badge badge = Badge.active(false);

        assertEquals("off", badge.getTone());
        assertEquals("Inactive", badge.getLabel());
    }

    @Test
    void bookingLabelsACancelledBooking()
    {
        Badge badge = Badge.booking(false);

        assertEquals("off", badge.getTone());
        assertEquals("Cancelled", badge.getLabel());
    }

    @Test
    void confirmedUsesTheOkTone()
    {
        assertEquals("ok", Badge.confirmed().getTone());
        assertEquals("Confirmed", Badge.confirmed().getLabel());
    }

    @Test
    void ofKeepsTheToneAndLabel()
    {
        Badge badge = Badge.of("warn", "Partial");

        assertEquals("warn", badge.getTone());
        assertEquals("Partial", badge.getLabel());
    }
}
