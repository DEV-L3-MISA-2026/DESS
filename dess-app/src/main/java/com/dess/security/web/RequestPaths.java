package com.dess.security.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the request path used by the security filters.
 *
 * <p>The servlet container reports the URI with the context path; the filters
 * compare application-relative paths such as {@code /login} or {@code /api/health}.</p>
 */
public final class RequestPaths
{
    private RequestPaths()
    {
    }

    /**
     * @param request current request
     * @return the request URI without the context path
     */
    public static String path(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String context = request.getContextPath();

        return uri != null && context != null && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
    }

    /**
     * @param request current request
     * @return {@code true} when the request targets the JSON API
     */
    public static boolean isApi(HttpServletRequest request)
    {
        return path(request).startsWith("/api/");
    }
}
