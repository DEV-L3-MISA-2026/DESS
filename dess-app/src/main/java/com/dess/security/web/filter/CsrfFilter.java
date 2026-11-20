package com.dess.security.web.filter;

import com.dess.security.web.CsrfTokens;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Rejects state-changing requests without a valid CSRF token.
 *
 * <p>Safe methods pass through; the token is created on the first GET so every form can
 * render it. Login is the only state-changing request that a caller may send before a
 * session exists, and that request still needs the token created by the login page.</p>
 */
public class CsrfFilter implements Filter
{
    private static final String LOGIN_PATH = "/login";
    private static final String LOGIN_API_PATH = "/api/auth/login";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = path(request);

        if (isSafe(request.getMethod()))
        {
            HttpSession session = request.getSession(false);

            if (session == null && LOGIN_PATH.equals(path))
            {
                session = request.getSession(true);
            }

            if (session != null)
            {
                request.setAttribute(CsrfTokens.REQUEST_ATTRIBUTE,
                        CsrfTokens.ensure(session));
            }

            chain.doFilter(req, res);
            return;
        }

        if (LOGIN_API_PATH.equals(path))
        {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);

        // Without a session there is nothing to protect; SecurityFilter answers the
        // request. The login form is the exception: it must carry a token.
        if (session == null && !LOGIN_PATH.equals(path))
        {
            chain.doFilter(req, res);
            return;
        }

        String supplied = request.getParameter(CsrfTokens.PARAMETER);

        if (supplied == null || supplied.isBlank())
        {
            supplied = request.getHeader(CsrfTokens.HEADER);
        }

        if (!CsrfTokens.matches(session, supplied))
        {
            reject(request, response);
            return;
        }

        chain.doFilter(req, res);
    }

    private static boolean isSafe(String method)
    {
        return "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method);
    }

    private static String path(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String context = request.getContextPath();

        return uri != null && context != null && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
    }

    private static void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        if (path(request).startsWith("/api/"))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"code\":\"FORBIDDEN\",\"message\":\"Invalid or missing CSRF token\"}");
        }
        else
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "The form expired or was not sent by this application. Reload the page and try again.");
        }
    }
}
