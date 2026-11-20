package com.dess.security.web.servlet;

import com.dess.security.application.AuthenticationService;
import com.dess.security.application.UserManagementService;
import com.dess.security.application.impl.LoginThrottle;
import com.dess.security.domain.UserAccount;
import com.dess.security.web.SessionUser;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

/**
 * Sign-in page and form handler.
 *
 * <p>The page is public; the filter maps an unauthenticated browser request here. A
 * successful login rotates the session id before the user is stored.</p>
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet
{
    private static final String LOGIN_VIEW = "/WEB-INF/views/auth/login.jsp";
    private static final String DEFAULT_TARGET = "/app";

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private LoginThrottle loginThrottle;

    @Inject
    private UserManagementService userManagementService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        if (currentUser(request) != null)
        {
            response.sendRedirect(request.getContextPath() + DEFAULT_TARGET);
            return;
        }

        request.setAttribute("next", safeNext(request, request.getParameter("next")));
        request.setAttribute("logout", request.getParameter("logout") != null);
        request.setAttribute("noAccounts", userManagementService.findAll().isEmpty());
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");

        String username = trim(request.getParameter("username"));
        String password = request.getParameter("password");
        String next = safeNext(request, request.getParameter("next"));
        String key = throttleKey(request, username);

        request.setAttribute("username", username);
        request.setAttribute("next", next);

        if (loginThrottle.isBlocked(key))
        {
            showError(request, response, 429,
                    "Too many failed attempts. Wait five minutes and try again.");
            return;
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty())
        {
            showError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Enter your username and password.");
            return;
        }

        Optional<UserAccount> account = authenticationService.authenticate(username, password);

        if (account.isEmpty())
        {
            loginThrottle.recordFailure(key);
            showError(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "The username or password is incorrect.");
            return;
        }

        loginThrottle.recordSuccess(key);

        HttpSession session = request.getSession(true);

        // Prevent session fixation: the identifier issued before login must not stay.
        request.changeSessionId();
        session.setAttribute(SessionUser.SESSION_ATTRIBUTE, SessionUser.from(account.get()));

        response.sendRedirect(next);
    }

    private void showError(HttpServletRequest request, HttpServletResponse response,
                           int status, String message) throws ServletException, IOException
    {
        response.setStatus(status);
        request.setAttribute("error", message);
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    private static SessionUser currentUser(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);

        return session == null
                ? null
                : (SessionUser) session.getAttribute(SessionUser.SESSION_ATTRIBUTE);
    }

    private static String throttleKey(HttpServletRequest request, String username)
    {
        String name = username == null ? "" : username.trim().toLowerCase();

        return name + "|" + request.getRemoteAddr();
    }

    /**
     * Keeps the return path inside this application.
     *
     * @param request current request
     * @param next requested path, may be null
     * @return a local path that starts with the context path
     */
    static String safeNext(HttpServletRequest request, String next)
    {
        String context = request.getContextPath();
        String fallback = context + DEFAULT_TARGET;

        if (next == null || next.isBlank())
        {
            return fallback;
        }

        if (!next.startsWith(context + "/") || next.startsWith(context + "//"))
        {
            return fallback;
        }

        return next;
    }

    private static String trim(String value)
    {
        return value == null ? null : value.trim();
    }
}
