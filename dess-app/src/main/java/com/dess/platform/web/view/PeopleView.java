package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Student list and creation form on the people page. */
public final class PeopleView implements PageView
{
    private final PageChrome chrome;
    private final List<PersonRow> students;
    private final String query;
    private final Map<String, String> form;
    private final Map<String, String> errors;
    private final String countLabel;
    private final EmptyState studentsEmpty;

    /**
     * @param students visible student rows
     * @param query current search text
     * @param canWrite whether the account may create a student
     * @param form submitted values, empty on a first visit
     * @param errors field errors, empty when the request succeeded
     */
    public PeopleView(List<PersonRow> students, String query, boolean canWrite,
                      Map<String, String> form, Map<String, String> errors)
    {
        PageChrome.Builder chrome = PageChrome.builder("People", "Directory", "people",
                        "/WEB-INF/views/people/index.jsp")
                .description("Students and their registration numbers. A person record is "
                        + "not a sign-in account.")
                .breadcrumb("Overview", "/app")
                .writable(canWrite);

        if (canWrite)
        {
            chrome.headerAction("Add student", "#create-student");
        }

        this.chrome = chrome.build();
        this.students = List.copyOf(students);
        this.query = Objects.requireNonNull(query, "query is required");
        this.form = Map.copyOf(form);
        this.errors = Map.copyOf(errors);
        this.countLabel = Views.count(students.size(), "record", "records") + " shown.";

        if (!query.isBlank())
        {
            this.studentsEmpty = EmptyState.of("No student matches this view.",
                    "Try a shorter search or clear the filter.");
        }
        else if (canWrite)
        {
            this.studentsEmpty = EmptyState.withAction("No student matches this view.",
                    "Create the first student to start the directory.",
                    "Add student", "/app/people#create-student");
        }
        else
        {
            this.studentsEmpty = EmptyState.of("No student matches this view.",
                    "Create the first student to start the directory.");
        }
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<PersonRow> getStudents()
    {
        return students;
    }

    public String getQuery()
    {
        return query;
    }

    public Map<String, String> getForm()
    {
        return form;
    }

    public Map<String, String> getErrors()
    {
        return errors;
    }

    public String getCountLabel()
    {
        return countLabel;
    }

    public EmptyState getStudentsEmpty()
    {
        return studentsEmpty;
    }
}
