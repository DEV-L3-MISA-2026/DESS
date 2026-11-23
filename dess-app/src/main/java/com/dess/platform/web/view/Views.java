package com.dess.platform.web.view;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * Helpers that render a {@link PageView}.
 *
 * <p>The page model is exposed as {@code page}. The shell fragments keep reading
 * the flat request attributes, so the institution, login and error pages do not
 * have to change.</p>
 */
public final class Views
{
    private Views()
    {
    }

    /**
     * Exposes the page shell and forwards to the view JSP.
     *
     * @param request current request
     * @param response current response
     * @param view page model
     * @throws ServletException when the forward fails
     * @throws IOException when the response cannot be written
     */
    public static void render(HttpServletRequest request, HttpServletResponse response,
                              PageView view) throws ServletException, IOException
    {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(response, "response is required");
        Objects.requireNonNull(view, "view is required");

        PageChrome chrome = view.getChrome();
        String context = request.getContextPath();

        request.setAttribute("page", view);
        request.setAttribute("pageTitle", chrome.getTitle());
        request.setAttribute("topbarContext", chrome.getTopbarContext());
        request.setAttribute("pageDescription", chrome.getDescription());
        request.setAttribute("activeNav", chrome.getActiveNav());
        request.setAttribute("breadcrumbParentLabel", chrome.getBreadcrumbParentLabel());
        request.setAttribute("breadcrumbParentHref",
                absolute(context, chrome.getBreadcrumbParentHref()));
        request.setAttribute("breadcrumbSectionLabel", chrome.getBreadcrumbSectionLabel());
        request.setAttribute("breadcrumbSectionHref",
                absolute(context, chrome.getBreadcrumbSectionHref()));
        request.setAttribute("headerActionLabel", chrome.getHeaderActionLabel());
        request.setAttribute("headerActionHref",
                absolute(context, chrome.getHeaderActionHref()));
        request.setAttribute("canWrite", chrome.isWritable());

        request.getRequestDispatcher(chrome.getViewPath()).forward(request, response);
    }

    /**
     * @param contextPath application context path, possibly empty
     * @param href application-relative path, anchor or empty value
     * @return the href with the context path when it starts with a slash
     */
    public static String absolute(String contextPath, String href)
    {
        Objects.requireNonNull(contextPath, "contextPath is required");

        if (href == null || href.isEmpty() || !href.startsWith("/"))
        {
            return href;
        }

        return contextPath + href;
    }

    /**
     * Pluralises a row count.
     *
     * @param count number of rows
     * @param singular noun for one row
     * @param plural noun for zero or many rows
     * @return the count and the matching noun, for example {@code 1 record} or
     *         {@code 3 records}
     * @throws IllegalArgumentException when the count is negative
     */
    public static String count(int count, String singular, String plural)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("count must not be negative");
        }

        return count + " " + (count == 1 ? singular : plural);
    }
}
