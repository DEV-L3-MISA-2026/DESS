package com.dess.platform.web;

import com.dess.security.application.AuthorizationService;
import com.dess.security.web.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small helpers shared by the server-rendered pages.
 *
 * <p>They keep servlets thin: read the current user, move flash messages from the
 * session to the request, and guard write actions.</p>
 */
public final class Web
{
    private static final String FLASH_KIND = "flashKind";
    private static final String FLASH_MESSAGE = "flashMessage";
    private static final DateTimeFormatter MOMENT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Web()
    {
    }

    /**
     * @param request current request
     * @return the signed-in user, or {@code null}
     */
    public static SessionUser currentUser(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);

        return session == null
                ? null
                : (SessionUser) session.getAttribute(SessionUser.SESSION_ATTRIBUTE);
    }

    /**
     * Stores a message for the next page.
     *
     * @param request current request
     * @param kind one of {@code success}, {@code error}, {@code warning}, {@code info}
     * @param message text to show
     */
    public static void flash(HttpServletRequest request, String kind, String message)
    {
        HttpSession session = request.getSession(true);
        session.setAttribute(FLASH_KIND, kind);
        session.setAttribute(FLASH_MESSAGE, message);
    }

    /**
     * Moves a stored flash message to the request and clears it.
     *
     * @param request current request
     */
    public static void exposeFlash(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);

        if (session == null)
        {
            return;
        }

        Object kind = session.getAttribute(FLASH_KIND);
        Object message = session.getAttribute(FLASH_MESSAGE);

        if (kind != null || message != null)
        {
            request.setAttribute(FLASH_KIND, kind);
            request.setAttribute(FLASH_MESSAGE, message);
            session.removeAttribute(FLASH_KIND);
            session.removeAttribute(FLASH_MESSAGE);
        }
    }

    /**
     * Checks a permission for the current user and writes an error response when it is
     * missing.
     *
     * @param request current request
     * @param response current response
     * @param authorization authorization service
     * @param permissionCode required permission
     * @return true when the request may continue
     * @throws IOException when the error response cannot be written
     */
    public static boolean requirePermission(HttpServletRequest request,
                                            HttpServletResponse response,
                                            AuthorizationService authorization,
                                            String permissionCode) throws IOException
    {
        SessionUser user = currentUser(request);

        if (user == null)
        {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (!canWrite(request, authorization, permissionCode))
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Your account cannot perform this action.");
            return false;
        }

        return true;
    }

    /**
     * Tells whether the signed-in user holds a write permission. Pages use it to
     * show or hide their write controls.
     *
     * @param request current request
     * @param authorization authorization service
     * @param permissionCode permission to test
     * @return true when a user is signed in and holds the permission
     */
    public static boolean canWrite(HttpServletRequest request,
                                   AuthorizationService authorization,
                                   String permissionCode)
    {
        SessionUser user = currentUser(request);

        return user != null && authorization.isPermitted(user.toAccount(), permissionCode);
    }

    /**
     * Reads a trimmed request parameter.
     *
     * @param request current request
     * @param name parameter name
     * @return the trimmed value, or an empty string
     */
    public static String param(HttpServletRequest request, String name)
    {
        String value = request.getParameter(name);

        return value == null ? "" : value.trim();
    }

    /**
     * Formats an instant the way the timetable pages display it.
     *
     * @param value instant to format, possibly with a non-UTC offset
     * @return {@code yyyy-MM-dd HH:mm} in UTC
     */
    public static String moment(OffsetDateTime value)
    {
        return MOMENT.format(value.withOffsetSameInstant(ZoneOffset.UTC));
    }

    /**
     * @return a mutable map for form values
     */
    public static Map<String, String> form()
    {
        return new LinkedHashMap<>();
    }
}
