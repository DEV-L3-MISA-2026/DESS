package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViewsTest
{
    @Test
    void countUsesTheSingularNounForOneRow()
    {
        assertEquals("1 record", Views.count(1, "record", "records"));
    }

    @Test
    void countUsesThePluralNounForZeroRows()
    {
        assertEquals("0 records", Views.count(0, "record", "records"));
    }

    @Test
    void countUsesThePluralNounForManyRows()
    {
        assertEquals("3 records", Views.count(3, "record", "records"));
    }

    @Test
    void countRejectsANegativeValue()
    {
        assertThrows(IllegalArgumentException.class,
                () -> Views.count(-1, "record", "records"));
    }

    @Test
    void absolutePrefixesApplicationRelativeHrefs()
    {
        assertEquals("/dess-app/app/people", Views.absolute("/dess-app", "/app/people"));
    }

    @Test
    void absoluteKeepsAnchorsAndExternalValues()
    {
        assertEquals("#create-student", Views.absolute("/dess-app", "#create-student"));
        assertEquals("https://example.test", Views.absolute("/dess-app",
                "https://example.test"));
    }

    @Test
    void absoluteKeepsEmptyValues()
    {
        assertEquals("", Views.absolute("/dess-app", ""));
        assertNull(Views.absolute("/dess-app", null));
    }
}
