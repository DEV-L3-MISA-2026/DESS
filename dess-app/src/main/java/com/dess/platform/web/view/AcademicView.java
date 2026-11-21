package com.dess.platform.web.view;

import java.util.List;
import java.util.Map;

/**
 * Periods, programs and courses with their creation forms.
 */
public final class AcademicView implements PageView
{
    private final PageChrome chrome;
    private final List<PeriodRow> periods;
    private final List<ProgramRow> programs;
    private final List<CourseRow> courses;
    private final Map<String, String> periodForm;
    private final Map<String, String> periodErrors;
    private final Map<String, String> programForm;
    private final Map<String, String> programErrors;
    private final Map<String, String> courseForm;
    private final Map<String, String> courseErrors;
    private final List<Option> termOptions;
    private final String periodCountLabel;
    private final String programCountLabel;
    private final String courseCountLabel;
    private final EmptyState periodsEmpty;
    private final EmptyState programsEmpty;
    private final EmptyState coursesEmpty;

    /**
     * @param periods academic periods
     * @param programs programs
     * @param courses courses
     * @param canWrite whether the account may create catalogue entries
     * @param periodForm submitted period values
     * @param periodErrors period field errors
     * @param programForm submitted program values
     * @param programErrors program field errors
     * @param courseForm submitted course values
     * @param courseErrors course field errors
     */
    public AcademicView(List<PeriodRow> periods, List<ProgramRow> programs,
                        List<CourseRow> courses, boolean canWrite,
                        Map<String, String> periodForm, Map<String, String> periodErrors,
                        Map<String, String> programForm, Map<String, String> programErrors,
                        Map<String, String> courseForm, Map<String, String> courseErrors)
    {
        this.chrome = PageChrome.builder("Academic", "Catalogue and calendar", "academic",
                        "/WEB-INF/views/academic/index.jsp")
                .description("Periods set the calendar; programs describe credentials; "
                        + "courses form the catalogue. A course is not yet an offering.")
                .breadcrumb("Overview", "/app")
                .writable(canWrite)
                .build();
        this.periods = List.copyOf(periods);
        this.programs = List.copyOf(programs);
        this.courses = List.copyOf(courses);
        this.periodForm = Map.copyOf(periodForm);
        this.periodErrors = Map.copyOf(periodErrors);
        this.programForm = Map.copyOf(programForm);
        this.programErrors = Map.copyOf(programErrors);
        this.courseForm = Map.copyOf(courseForm);
        this.courseErrors = Map.copyOf(courseErrors);
        String selectedTerm = periodForm.get("term");
        this.termOptions = List.of(
                Option.of("1", "1", "1".equals(selectedTerm)),
                Option.of("2", "2", "2".equals(selectedTerm)),
                Option.of("3", "3", "3".equals(selectedTerm)));
        this.periodCountLabel = Views.count(periods.size(), "period", "periods") + ".";
        this.programCountLabel = Views.count(programs.size(), "program", "programs") + ".";
        this.courseCountLabel = Views.count(courses.size(), "course", "courses")
                + " in the catalogue.";
        this.periodsEmpty = EmptyState.of("No academic period yet.",
                "A period is the year and term that academic operations refer to.");
        this.programsEmpty = EmptyState.of("No program yet.",
                "Create a program before adding its versions and enrollments.");
        this.coursesEmpty = EmptyState.of("No course yet.",
                "Add the first course to the catalogue.");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<PeriodRow> getPeriods()
    {
        return periods;
    }

    public List<ProgramRow> getPrograms()
    {
        return programs;
    }

    public List<CourseRow> getCourses()
    {
        return courses;
    }

    public Map<String, String> getPeriodForm()
    {
        return periodForm;
    }

    public Map<String, String> getPeriodErrors()
    {
        return periodErrors;
    }

    public Map<String, String> getProgramForm()
    {
        return programForm;
    }

    public Map<String, String> getProgramErrors()
    {
        return programErrors;
    }

    public Map<String, String> getCourseForm()
    {
        return courseForm;
    }

    public Map<String, String> getCourseErrors()
    {
        return courseErrors;
    }

    public List<Option> getTermOptions()
    {
        return termOptions;
    }

    public String getPeriodCountLabel()
    {
        return periodCountLabel;
    }

    public String getProgramCountLabel()
    {
        return programCountLabel;
    }

    public String getCourseCountLabel()
    {
        return courseCountLabel;
    }

    public EmptyState getPeriodsEmpty()
    {
        return periodsEmpty;
    }

    public EmptyState getProgramsEmpty()
    {
        return programsEmpty;
    }

    public EmptyState getCoursesEmpty()
    {
        return coursesEmpty;
    }
}
