package com.dess.platform.web.view;

import java.util.Objects;

/**
 * One item in a {@code select}, datalist or radio group.
 *
 * <p>{@code selectedAttribute} is the literal {@code selected} attribute or an
 * empty string, so a JSP does not need a conditional expression.</p>
 */
public final class Option
{
    private final String value;
    private final String label;
    private final boolean selected;

    private Option(String value, String label, boolean selected)
    {
        this.value = Objects.requireNonNull(value, "value is required");
        this.label = Objects.requireNonNull(label, "label is required");
        this.selected = selected;
    }

    /**
     * @param value submitted value
     * @param label visible text
     * @return an unselected option
     */
    public static Option of(String value, String label)
    {
        return new Option(value, label, false);
    }

    /**
     * @param value submitted value
     * @param label visible text
     * @param selected whether to render the selected attribute
     * @return the option
     */
    public static Option of(String value, String label, boolean selected)
    {
        return new Option(value, label, selected);
    }

    public String getValue()
    {
        return value;
    }

    public String getLabel()
    {
        return label;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public String getSelectedAttribute()
    {
        return selected ? "selected" : "";
    }
}
