package com.dess.platform.web.view;

import java.util.Objects;

/**
 * One figure on the overview page.
 *
 * <p>{@code href} is application-relative and starts with a slash.</p>
 */
public final class StatCard
{
    private final String label;
    private final int value;
    private final String note;
    private final String href;

    /**
     * @param label card heading
     * @param value number to show
     * @param note short explanation
     * @param href application-relative destination
     */
    public StatCard(String label, int value, String note, String href)
    {
        this.label = Objects.requireNonNull(label, "label is required");
        this.value = value;
        this.note = Objects.requireNonNull(note, "note is required");
        this.href = Objects.requireNonNull(href, "href is required");

        if (!href.startsWith("/"))
        {
            throw new IllegalArgumentException("href must be application-relative");
        }
    }

    public String getLabel()
    {
        return label;
    }

    public int getValue()
    {
        return value;
    }

    public String getNote()
    {
        return note;
    }

    public String getHref()
    {
        return href;
    }
}
