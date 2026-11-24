package com.dess.platform.web.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeopleViewTest
{
    @Test
    void countLabelUsesTheSingularForOneStudent()
    {
        PeopleView view = new PeopleView(
                List.of(new PersonRow("Ada Lovelace", "ada@example.test", "S-1", true)),
                "", false, Map.of(), Map.of());

        assertEquals("1 record shown.", view.getCountLabel());
    }

    @Test
    void aWritablePageOffersTheCreateAction()
    {
        PeopleView view = new PeopleView(List.of(), "", true, Map.of(), Map.of());

        assertEquals("Add student", view.getChrome().getHeaderActionLabel());
        assertEquals("#create-student", view.getChrome().getHeaderActionHref());
        assertTrue(view.getChrome().isWritable());
    }

    @Test
    void aReadOnlyPageHasNoHeaderAction()
    {
        PeopleView view = new PeopleView(List.of(), "", false, Map.of(), Map.of());

        assertEquals("", view.getChrome().getHeaderActionLabel());
        assertEquals("", view.getChrome().getHeaderActionHref());
    }

    @Test
    void anEmptyWritableDirectoryLinksToTheCreateForm()
    {
        PeopleView view = new PeopleView(List.of(), "", true, Map.of(), Map.of());

        assertEquals("Add student", view.getStudentsEmpty().getActionLabel());
        assertEquals("/app/people#create-student", view.getStudentsEmpty().getActionHref());
    }

    @Test
    void aSearchWithNoMatchDoesNotOfferTheCreateAction()
    {
        PeopleView view = new PeopleView(List.of(), "zzz", true, Map.of(), Map.of());

        assertEquals("", view.getStudentsEmpty().getActionLabel());
        assertEquals("Try a shorter search or clear the filter.",
                view.getStudentsEmpty().getMessage());
    }
}
