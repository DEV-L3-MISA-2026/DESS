package com.dess.security.web.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Signs the current user out.
 *
 * <p>Only POST is accepted so a link or an image cannot end the session. The CSRF
 * filter validates the token before this servlet runs.</p>
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet
{
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        HttpSession session = request.getSession(false);

        if (session != null)
        {
            session.invalidate();
        }

        response.sendRedirect(request.getContextPath() + "/login?logout=1");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Sign out requires a POST request.");
    }
}
