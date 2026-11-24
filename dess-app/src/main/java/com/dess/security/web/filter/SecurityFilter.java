package com.dess.security.web.filter;

import com.dess.security.domain.RoleCatalog;
import com.dess.security.web.RequestPaths;
import com.dess.security.web.SessionUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Requires a signed-in user for application pages and the REST API.
 *
 * <p>API callers receive JSON errors; browser requests are redirected to the login page
 * with a safe return path. The security area also requires the administrator role, and
 * changing the institution structure requires the {@code institution:write} permission.</p>
 */
public class SecurityFilter implements Filter
{
    private static final String LOGIN_PATH = "/login";
    private static final String LOGIN_API_PATH = "/api/auth/login";
    private static final String LOGOUT_PATH = "/logout";
    private static final String HEALTH_PATH = "/api/health";
    private static final String SECURITY_AREA = "/app/security";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = RequestPaths.path(request);

        addSecurityHeaders(response);

        if (estRoutePublique(path))
        {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        SessionUser user = session == null
                ? null
                : (SessionUser) session.getAttribute(SessionUser.SESSION_ATTRIBUTE);

        if (user == null)
        {
            denyUnauthenticated(request, response);
            return;
        }

        if (path.startsWith(SECURITY_AREA) && !user.isPermitted(RoleCatalog.SECURITY_MANAGE))
        {
            denyForbidden(request, response);
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * Checks whether a request tries to change the institution structure.
     */
    public boolean estEcritureInstitution(String path, String method)
    {
        if (path == null || method == null)
        {
            return false;
        }

        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method))
        {
            return false;
        }

        return !path.equals(LOGIN_PATH)
                && !path.equals(LOGOUT_PATH)
                && !path.startsWith("/api/")
                && !path.startsWith("/app/")
                && !path.startsWith("/assets/")
                && !path.equals("/favicon.ico");
    }

    /**
     * Checks whether a path is available without a session.
     */
    public boolean estRoutePublique(String path)
    {
        if (path == null)
        {
            return false;
        }

        return path.equals(LOGIN_PATH)
                || path.equals(LOGIN_API_PATH)
                || path.equals(HEALTH_PATH)
                || path.startsWith("/assets/")
                || path.equals("/favicon.ico");
    }

    /** Alias for compatibility with existing tests. */
    public boolean isRoutePublic(String chemin)
    {
        return estRoutePublique(chemin);
    }

    public static String construireJson401()
    {
        return "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}";
    }

    public static String construireJson403()
    {
        return "{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}";
    }

    private static void denyUnauthenticated(HttpServletRequest request,
                                            HttpServletResponse response)
            throws IOException
    {
        if (RequestPaths.isApi(request))
        {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, construireJson401());
            return;
        }

        String target = request.getRequestURI();
        String query = request.getQueryString();

        if (query != null && !query.isBlank())
        {
            target = target + "?" + query;
        }

        String login = request.getContextPath() + LOGIN_PATH;
        response.sendRedirect(login + "?next="
                + URLEncoder.encode(target, StandardCharsets.UTF_8));
    }

    private static void denyForbidden(HttpServletRequest request,
                                      HttpServletResponse response)
            throws IOException
    {
        if (RequestPaths.isApi(request))
        {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, construireJson403());
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private static void writeJson(HttpServletResponse response, int status, String body)
            throws IOException
    {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }

    private static void addSecurityHeaders(HttpServletResponse response)
    {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; "
                        + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                        + "font-src 'self' data:; base-uri 'self'; form-action 'self'");
    }
}
