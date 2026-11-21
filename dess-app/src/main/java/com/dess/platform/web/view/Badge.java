package com.dess.platform.web.view;

import java.util.Objects;

/**
 * A status badge with a design-system tone and a visible label.
 *
 * <p>The factories keep the tone and the text together, so a JSP never chooses
 * a colour or repeats a status word.</p>
 */
public final class Badge
{
    private final String tone;
    private final String label;

    private Badge(String tone, String label)
    {
        this.tone = Objects.requireNonNull(tone, "tone is required");
        this.label = Objects.requireNonNull(label, "label is required");
    }

    /**
     * @param active whether the record is active
     * @return {@code Active} or {@code Inactive}
     */
    public static Badge active(boolean active)
    {
        return active ? of("ok", "Active") : of("off", "Inactive");
    }

    /**
     * @param active whether the booking is still active
     * @return {@code Active} or {@code Cancelled}
     */
    public static Badge booking(boolean active)
    {
        return active ? of("ok", "Active") : of("off", "Cancelled");
    }

    /**
     * @return the badge for a stored exam session
     */
    public static Badge confirmed()
    {
        return of("ok", "Confirmed");
    }

    /**
     * @param tone one of {@code ok}, {@code warn}, {@code info}, {@code danger} or
     *             {@code off}
     * @param label visible text
     * @return a badge with the given tone and label
     */
    public static Badge of(String tone, String label)
    {
        return new Badge(tone, label);
    }

    public String getTone()
    {
        return tone;
    }

    public String getLabel()
    {
        return label;
    }
}
