package com.dess.platform.web.view;

/**
 * A server-rendered page model.
 *
 * <p>A page view owns the values a JSP prints: the shell chrome, count labels,
 * empty states, badges and typed rows. Servlets build one and pass it to
 * {@link Views#render}, so the JSP contains markup rather than presentation
 * logic.</p>
 */
public interface PageView
{
    /**
     * @return the page shell: title, breadcrumb trail and header action
     */
    PageChrome getChrome();
}
