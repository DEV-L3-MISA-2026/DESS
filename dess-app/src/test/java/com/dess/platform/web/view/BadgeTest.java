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

    @Test
    void eligibleUsesTheOkToneAndIneligibleTheDangerTone()
    {
        assertEquals("ok", Badge.eligible(true).getTone());
        assertEquals("Eligible", Badge.eligible(true).getLabel());
        assertEquals("danger", Badge.eligible(false).getTone());
        assertEquals("Not eligible", Badge.eligible(false).getLabel());
    }

    @Test
    void resultStatusLabelsEachStoredStatus()
    {
        assertEquals("ok", Badge.resultStatus(
                com.dess.assessment.domain.ResultStatus.SCORED).getTone());
        assertEquals("Scored", Badge.resultStatus(
                com.dess.assessment.domain.ResultStatus.SCORED).getLabel());
        assertEquals("off", Badge.resultStatus(
                com.dess.assessment.domain.ResultStatus.ABSENT).getTone());
        assertEquals("Absent", Badge.resultStatus(
                com.dess.assessment.domain.ResultStatus.ABSENT).getLabel());
        assertEquals("info", Badge.resultStatus(
                com.dess.assessment.domain.ResultStatus.EXCUSED).getTone());
        assertEquals("Excused", Badge.resultStatus(
                com.dess.assessment.domain.ResultStatus.EXCUSED).getLabel());
    }
}
