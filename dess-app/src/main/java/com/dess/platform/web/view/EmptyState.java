package com.dess.platform.web.view;

import java.util.Objects;

/**
 * Contents of an empty-state block.
 *
 * <p>{@code actionHref} is application-relative and starts with a slash when it
 * is present. The shared fragment adds the context path.</p>
 */
public final class EmptyState
{
    private final String title;
    private final String message;
    private final String actionLabel;
    private final String actionHref;

    private EmptyState(String title, String message, String actionLabel, String actionHref)
    {
        this.title = Objects.requireNonNull(title, "title is required");
        this.message = Objects.requireNonNull(message, "message is required");
        this.actionLabel = Objects.requireNonNull(actionLabel, "actionLabel is required");
        this.actionHref = Objects.requireNonNull(actionHref, "actionHref is required");
    }

    /**
     * @param title short state title
     * @param message explanation
     * @return an empty state without an action
     */
    public static EmptyState of(String title, String message)
    {
        return new EmptyState(title, message, "", "");
    }

    /**
     * @param title short state title
     * @param message explanation
     * @param actionLabel action text
     * @param actionHref application-relative action path
     * @return an empty state with an action
     */
    public static EmptyState withAction(String title, String message, String actionLabel,
                                        String actionHref)
    {
        if (!actionHref.startsWith("/"))
        {
            throw new IllegalArgumentException("actionHref must be application-relative");
        }

        return new EmptyState(title, message, actionLabel, actionHref);
    }

    public String getTitle()
    {
        return title;
    }

    public String getMessage()
    {
        return message;
    }

    public String getActionLabel()
    {
        return actionLabel;
    }

    public String getActionHref()
    {
        return actionHref;
    }
}
