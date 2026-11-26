package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionTest
{
    @Test
    void anUnselectedOptionHasNoSelectedAttribute()
    {
        Option option = Option.of("1", "Term 1");

        assertEquals("1", option.getValue());
        assertEquals("Term 1", option.getLabel());
        assertEquals("", option.getSelectedAttribute());
    }

    @Test
    void aSelectedOptionRendersTheSelectedAttribute()
    {
        Option option = Option.of("1", "Term 1", true);

        assertEquals("selected", option.getSelectedAttribute());
    }
}
