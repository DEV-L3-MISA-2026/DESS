package com.dess.platform.web.view;

import java.util.Objects;

/**
 * Immutable page shell: document title, top-bar context, breadcrumb trail and the
 * optional header action.
 *
 * <p>Hrefs are application-relative. {@link Views#render} adds the context path
 * when it forwards the page, so view models do not need the request.</p>
 */
public record PageChrome(
        String title,
        String topbarContext,
        String activeNav,
        String description,
        String viewPath,
        String breadcrumbParentLabel,
        String breadcrumbParentHref,
        String breadcrumbSectionLabel,
        String breadcrumbSectionHref,
        String headerActionLabel,
        String headerActionHref,
        boolean writable)
{
    public PageChrome
    {
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(topbarContext, "topbarContext is required");
        Objects.requireNonNull(activeNav, "activeNav is required");
        Objects.requireNonNull(description, "description is required");
        Objects.requireNonNull(viewPath, "viewPath is required");
        Objects.requireNonNull(breadcrumbParentLabel, "breadcrumb parent label is required");
        Objects.requireNonNull(breadcrumbParentHref, "breadcrumb parent href is required");
        Objects.requireNonNull(breadcrumbSectionLabel, "breadcrumb section label is required");
        Objects.requireNonNull(breadcrumbSectionHref, "breadcrumb section href is required");
        Objects.requireNonNull(headerActionLabel, "header action label is required");
        Objects.requireNonNull(headerActionHref, "header action href is required");
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
            this.title = title;
            this.topbarContext = topbarContext;
            this.activeNav = activeNav;
            this.viewPath = viewPath;
        }

        public Builder description(String description)
        {
            this.description = description;
            return this;
        }

        public Builder breadcrumb(String label, String href)
        {
            this.breadcrumbParentLabel = label;
            this.breadcrumbParentHref = href;
            return this;
        }

        public Builder section(String label, String href)
        {
            this.breadcrumbSectionLabel = label;
            this.breadcrumbSectionHref = href;
            return this;
        }

        public Builder headerAction(String label, String href)
        {
            this.headerActionLabel = label;
            this.headerActionHref = href;
            return this;
        }

        public Builder writable(boolean writable)
        {
            this.writable = writable;
            return this;
        }

        public PageChrome build()
        {
            return new PageChrome(title, topbarContext, activeNav, description, viewPath,
                    breadcrumbParentLabel, breadcrumbParentHref, breadcrumbSectionLabel,
                    breadcrumbSectionHref, headerActionLabel, headerActionHref, writable);
        }
    }
}
