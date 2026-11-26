package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Assessments page: the filtered list of assessment definitions and the
 * create form.
 *
 * <p>The list can be filtered by type and by course. Both filters live in
 * the URL, so reloading the URL restores them. There is no period filter:
 * an assessment has no period until issue #18 (course offerings) lands,
 * and no period is invented here.</p>
 */
public final class AssessmentsView implements PageView
{
    private final PageChrome chrome;
    private final List<AssessmentRow> assessments;
    private final List<Option> typeOptions;
    private final List<Option> courseOptions;
    private final List<Option> createCourses;
    private final List<Option> createTypes;
    private final String typeFilter;
    private final String courseFilter;
    private final Map<String, String> form;
    private final Map<String, String> errors;
    private final String countLabel;
    private final EmptyState assessmentsEmpty;

    /**
     * @param assessments visible assessment rows, already filtered
     * @param typeOptions type choices, with the active filter selected
     * @param courseOptions course choices, with the active filter selected
     * @param createCourses course choices for the create form, with the submitted
     *                      value selected
     * @param createTypes type choices for the create form, with the submitted
     *                    value selected
     * @param typeFilter current type filter value, or an empty string
     * @param courseFilter current course filter value, or an empty string
     * @param canWrite whether the account may create an assessment
     * @param form submitted create values, kept after a validation error
     * @param errors create field errors, keyed by the field DOM id
     */
    public AssessmentsView(List<AssessmentRow> assessments, List<Option> typeOptions,
                           List<Option> courseOptions, List<Option> createCourses,
                           List<Option> createTypes, String typeFilter, String courseFilter,
                           boolean canWrite, Map<String, String> form, Map<String, String> errors)
    {
        this.chrome = PageChrome.builder("Assessments", "Examinations", "assessments",
                        "/WEB-INF/views/assessments/index.jsp")
                .description("Assessment definitions: what is evaluated, for which course, "
                        + "with which maximum score and duration in minutes. "
                        + "An assessment is never deleted; it is deactivated when it must no "
                        + "longer accept new results.")
                .breadcrumb("Overview", "/app")
                .section("Examinations", "/app/examinations")
                .headerAction("View examinations", "/app/examinations")
                .writable(canWrite)
                .build();
        this.assessments = List.copyOf(assessments);
        this.typeOptions = List.copyOf(typeOptions);
        this.courseOptions = List.copyOf(courseOptions);
        this.createCourses = List.copyOf(createCourses);
        this.createTypes = List.copyOf(createTypes);
        this.typeFilter = Objects.requireNonNull(typeFilter, "typeFilter is required");
        this.courseFilter = Objects.requireNonNull(courseFilter, "courseFilter is required");
        this.form = Map.copyOf(form);
        this.errors = Map.copyOf(errors);
        this.countLabel = Views.count(assessments.size(), "assessment", "assessments")
                + " shown. Duration is in minutes.";
        boolean filtered = !typeFilter.isBlank() || !courseFilter.isBlank();

        if (filtered)
        {
            this.assessmentsEmpty = EmptyState.withAction(
                    "No assessment matches these filters.",
                    "Try a different type or course, or clear the filters to see every "
                            + "assessment.",
                    "Clear filters", "/app/assessments");
        }
        else if (canWrite)
        {
            this.assessmentsEmpty = EmptyState.of("No assessment yet.",
                    "Create the first assessment below.");
        }
        else
        {
            this.assessmentsEmpty = EmptyState.of("No assessment yet.",
                    "No assessment definition has been created yet.");
        }
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<AssessmentRow> getAssessments()
    {
        return assessments;
    }

    public List<Option> getTypeOptions()
    {
        return typeOptions;
    }

    public List<Option> getCourseOptions()
    {
        return courseOptions;
    }

    public List<Option> getCreateCourses()
    {
        return createCourses;
    }

    public List<Option> getCreateTypes()
    {
        return createTypes;
    }

    public String getTypeFilter()
    {
        return typeFilter;
    }

    public String getCourseFilter()
    {
        return courseFilter;
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

    public EmptyState getAssessmentsEmpty()
    {
        return assessmentsEmpty;
    }
}
