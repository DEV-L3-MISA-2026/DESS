package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmptyStateTest
{
    @Test
    void ofBuildsAStateWithoutAnAction()
    {
        EmptyState state = EmptyState.of("Nothing here.", "Add a record first.");

        assertEquals("Nothing here.", state.getTitle());
        assertEquals("Add a record first.", state.getMessage());
        assertEquals("", state.getActionLabel());
        assertEquals("", state.getActionHref());
    }

    @Test
    void withActionKeepsAnApplicationRelativeHref()
    {
        EmptyState state = EmptyState.withAction("Nothing here.", "Add a record first.",
                "Add record", "/app/people#create-student");

        assertEquals("Add record", state.getActionLabel());
        assertEquals("/app/people#create-student", state.getActionHref());
    }

    @Test
    void withActionRejectsANonRelativeHref()
    {
        assertThrows(IllegalArgumentException.class,
                () -> EmptyState.withAction("Nothing here.", "Add a record first.",
                        "Add record", "app/people"));
    }
}
