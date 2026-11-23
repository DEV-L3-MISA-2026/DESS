package com.dess.security.web.filter;

import com.dess.security.domain.RoleCatalog;
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
        String path = path(request);

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

        // Vérifier si la route est publique (pas besoin d'être connecté)
        if (isRoutePublic(chemin)) {
            chain.doFilter(req, res); // laisser passer
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * Checks whether a request tries to change the institution structure.
     *
     * <p>Writes outside the application, API, login and static areas belong to the
     * institution editor. Treating them as institution writes keeps the rule safe when a
     * container maps a path parameter, an encoded segment or a doubled slash onto the
     * editor. Known areas keep their own checks.</p>
     *
     * @param path path relative to the application context
     * @param method HTTP method of the request
     * @return true when the request needs {@code institution:write}
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
     *
     * @param path path relative to the application context
     * @return true for the login page, the login API, health and static assets
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

    public String construireJson401()
    {
        return "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}";
    }

    public String construireJson403()
    {
        return "{\"code\":\"FORBIDDEN\",\"message\":\"Access denied\"}";
    }

    // Vérifie si l'URL correspond à une route publique
    // Extrait pour faciliter les tests unitaires
    public boolean isRoutePublic(String chemin) {
        for (String route : ROUTES_PUBLIQUES) {
            if (chemin != null && chemin.endsWith(route)) {
                return true;
            }
        }

        // Keep the context path: LoginServlet.safeNext() only accepts local paths
        // that start with it, so a stripped path would fall back to the overview.
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

    // Alias en français pour compatibilité avec le code de test existant
    public boolean estRoutePublique(String chemin) {
        return isRoutePublic(chemin);
    }

    // Envoie une réponse 401 en JSON
    // Extrait pour faciliter les tests unitaires
    public String construireJson401() {
        return "{\"code\":\"UNAUTHORIZED\",\"message\":\"Connexion requise\"}";
    }

    private static void writeJson(HttpServletResponse response, int status, String body)
            throws IOException
    {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }

    private static boolean isApi(HttpServletRequest request)
    {
        return path(request).startsWith("/api/");
    }

    private static String path(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String context = request.getContextPath();

        return uri != null && context != null && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
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
