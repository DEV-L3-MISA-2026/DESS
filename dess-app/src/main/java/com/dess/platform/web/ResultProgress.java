package com.dess.platform.web;

/**
 * Result completion for one exam session, as shown on the results page.
 *
 * <p>The label says how many results are recorded, not how many candidates
 * passed. It never turns a missing result into a mark.</p>
 *
 * @param label visible text
 * @param tone badge tone: {@code ok}, {@code warn}, {@code info} or {@code off}
 */
public record ResultProgress(String label, String tone)
{
    /**
     * @param candidates number of candidates linked to the session
     * @param recorded number of results recorded for the session's assessment
     * @return the label and tone for the session row
     * @throws IllegalArgumentException when a count is negative
     */
    public static ResultProgress of(int candidates, int recorded)
    {
        if (candidates < 0)
        {
            throw new IllegalArgumentException("candidates must not be negative");
        }
        if (recorded < 0)
        {
            throw new IllegalArgumentException("recorded must not be negative");
        }

        if (candidates == 0 && recorded == 0)
        {
            return new ResultProgress("No candidate yet", "info");
        }
        if (recorded == 0)
        {
            return new ResultProgress("Not entered", "off");
        }
        if (recorded > candidates)
        {
            return new ResultProgress(recorded + " recorded", "warn");
        }

        String label = recorded + " of " + candidates + " entered";

        return new ResultProgress(label, recorded == candidates ? "ok" : "warn");
    }
}
