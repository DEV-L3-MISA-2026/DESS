package com.dess.platform.web;

import com.dess.platform.web.view.AccountRow;
import com.dess.platform.web.view.SecurityView;
import com.dess.platform.web.view.Views;
import com.dess.security.application.AuthorizationService;
import com.dess.security.application.UserManagementService;
import com.dess.security.domain.RoleCatalog;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Account administration.
 *
 * <p>Only the administrator role may open this page. Password hashes are never sent to
 * the view; the list shows usernames, display names, roles and the active flag.</p>
 */
@WebServlet("/app/security")
public class SecurityServlet extends HttpServlet
{
    private static final String LIST_PATH = "/app/security";

    @Inject
    private UserManagementService userManagementService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        render(request, response, Web.form(), Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");

        if (!Web.requirePermission(request, response, authorizationService,
                RoleCatalog.SECURITY_MANAGE))
        {
            return;
        }

        Map<String, String> form = Web.form();
        form.put("username", Web.param(request, "username"));
        form.put("displayName", Web.param(request, "displayName"));
        String password = request.getParameter("password");
        String role = Web.param(request, "role");
        Map<String, String> errors = new LinkedHashMap<>();

        try
        {
            userManagementService.create(form.get("username"), password,
                    form.get("displayName"), Set.of(role));
            Web.flash(request, "success",
                    "Account " + form.get("username") + " was created.");
            response.sendRedirect(request.getContextPath() + LIST_PATH);
        }
        catch (IllegalArgumentException exception)
        {
            if (form.get("username").isBlank())
            {
                errors.put("username", "Enter a username.");
            }
            if (form.get("displayName").isBlank())
            {
                errors.put("displayName", "Enter a display name.");
            }
            if (password == null || password.isBlank())
            {
                errors.put("password", "Enter an initial password.");
            }
            if (role.isBlank())
            {
                errors.put("role", "Choose a role.");
            }
            errors.putIfAbsent("form", exception.getMessage());
            renderError(request, response, form, errors);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response,
                        Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        List<AccountRow> accounts = userManagementService.findAll().stream()
                .map(account -> new AccountRow(
                        account.username(),
                        account.displayName(),
                        account.roles().stream()
                                .map(role -> role.code())
                                .collect(Collectors.joining(", ")),
                        account.active()))
                .toList();

        SecurityView view = new SecurityView(accounts, form, errors);

        Views.render(request, response, view);
    }

    private void renderError(HttpServletRequest request, HttpServletResponse response,
                             Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        render(request, response, form, errors);
    }
}
