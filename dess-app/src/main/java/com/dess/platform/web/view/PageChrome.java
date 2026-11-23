package com.dess.platform.web.view;

import java.util.Objects;

/**
 * Immutable page shell: document title, top-bar context, breadcrumb trail and the
 * optional header action.
 *
 * <p>Hrefs are application-relative. {@link Views#render} adds the context path
 * when it forwards the page, so view models do not need the request.</p>
 */
public final class PageChrome
{
    private final String title;
    private final String topbarContext;
    private final String activeNav;
    private final String description;
    private final String viewPath;
    private final String breadcrumbParentLabel;
    private final String breadcrumbParentHref;
    private final String breadcrumbSectionLabel;
    private final String breadcrumbSectionHref;
    private final String headerActionLabel;
    private final String headerActionHref;
    private final boolean writable;

    private PageChrome(Builder builder)
    {
        this.title = builder.title;
        this.topbarContext = builder.topbarContext;
        this.activeNav = builder.activeNav;
        this.description = builder.description;
        this.viewPath = builder.viewPath;
        this.breadcrumbParentLabel = builder.breadcrumbParentLabel;
        this.breadcrumbParentHref = builder.breadcrumbParentHref;
        this.breadcrumbSectionLabel = builder.breadcrumbSectionLabel;
        this.breadcrumbSectionHref = builder.breadcrumbSectionHref;
        this.headerActionLabel = builder.headerActionLabel;
        this.headerActionHref = builder.headerActionHref;
        this.writable = builder.writable;
    }

    /**
     * @param title document and page title
     * @param topbarContext short text after the title in the top bar
     * @param activeNav sidebar item to mark as current
     * @param viewPath JSP under {@code /WEB-INF/views}
     * @return a builder with required values set
     */
    public static Builder builder(String title, String topbarContext, String activeNav,
                                  String viewPath)
    {
        return new Builder(title, topbarContext, activeNav, viewPath);
    }

    public String getTitle()
    {
        return title;
    }

    public String getTopbarContext()
    {
        return topbarContext;
    }

    public String getActiveNav()
    {
        return activeNav;
    }

    public String getDescription()
    {
        return description;
    }

    public String getViewPath()
    {
        return viewPath;
    }

    public String getBreadcrumbParentLabel()
    {
        return breadcrumbParentLabel;
    }

    public String getBreadcrumbParentHref()
    {
        return breadcrumbParentHref;
    }

    public String getBreadcrumbSectionLabel()
    {
        return breadcrumbSectionLabel;
    }

    public String getBreadcrumbSectionHref()
    {
        return breadcrumbSectionHref;
    }

    public String getHeaderActionLabel()
    {
        return headerActionLabel;
    }

    public String getHeaderActionHref()
    {
        return headerActionHref;
    }

    public boolean isWritable()
    {
        return writable;
    }

    /** Mutable builder for {@link PageChrome}. */
    public static final class Builder
    {
        private final String title;
        private final String topbarContext;
        private final String activeNav;
        private final String viewPath;
        private String description = "";
        private String breadcrumbParentLabel = "";
        private String breadcrumbParentHref = "";
        private String breadcrumbSectionLabel = "";
        private String breadcrumbSectionHref = "";
        private String headerActionLabel = "";
        private String headerActionHref = "";
        private boolean writable;

        private Builder(String title, String topbarContext, String activeNav, String viewPath)
        {
            this.title = Objects.requireNonNull(title, "title is required");
            this.topbarContext = Objects.requireNonNull(topbarContext,
                    "topbarContext is required");
            this.activeNav = Objects.requireNonNull(activeNav, "activeNav is required");
            this.viewPath = Objects.requireNonNull(viewPath, "viewPath is required");
        }

        public Builder description(String description)
        {
            this.description = Objects.requireNonNull(description,
                    "description is required");
            return this;
        }

        public Builder breadcrumb(String label, String href)
        {
            this.breadcrumbParentLabel = Objects.requireNonNull(label,
                    "breadcrumb label is required");
            this.breadcrumbParentHref = Objects.requireNonNull(href,
                    "breadcrumb href is required");
            return this;
        }

        public Builder section(String label, String href)
        {
            this.breadcrumbSectionLabel = Objects.requireNonNull(label,
                    "section label is required");
            this.breadcrumbSectionHref = Objects.requireNonNull(href,
                    "section href is required");
            return this;
        }

        public Builder headerAction(String label, String href)
        {
            this.headerActionLabel = Objects.requireNonNull(label,
                    "header action label is required");
            this.headerActionHref = Objects.requireNonNull(href,
                    "header action href is required");
            return this;
        }

        public Builder writable(boolean writable)
        {
            this.writable = writable;
            return this;
        }

        public PageChrome build()
        {
            return new PageChrome(this);
        }
    }
}
