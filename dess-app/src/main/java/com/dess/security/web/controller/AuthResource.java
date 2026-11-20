package com.dess.security.web.controller;

import com.dess.security.application.AuthenticationService;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
import com.dess.security.web.SessionUser;
import com.dess.shared.ApiError;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for authentication.
 *
 * <p>Endpoints: {@code POST /api/auth/login} creates an HTTP session and
 * {@code POST /api/auth/logout} invalidates it. The session stores only the
 * session-safe user view; the password hash never leaves the repository.</p>
 */
@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource
{
    @Inject
    private AuthenticationService authenticationService;

    @Context
    private HttpServletRequest httpRequest;

    public record LoginRequest(String username, String password)
    {
    }

    public record LoginResponse(String username, String displayName, Set<String> roles)
    {
    }

    public record LogoutResponse(String message)
    {
    }

    public AuthResource()
    {
    }

    public AuthResource(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    public AuthResource(AuthenticationService authenticationService,
                        HttpServletRequest httpRequest)
    {
        this.authenticationService = authenticationService;
        this.httpRequest = httpRequest;
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request)
    {
        if (request == null || request.username() == null || request.password() == null)
        {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("INVALID_INPUT",
                            "Username and password are required"))
                    .build();
        }

        Optional<UserAccount> account = authenticationService.authenticate(
                request.username(), request.password());

        if (account.isEmpty())
        {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiError("UNAUTHORIZED", "Invalid credentials"))
                    .build();
        }

        UserAccount authenticated = account.get();

        if (httpRequest != null)
        {
            HttpSession session = httpRequest.getSession(true);

            // Rotate the identifier so a session fixed before login cannot be reused.
            httpRequest.changeSessionId();
            session.setAttribute(SessionUser.SESSION_ATTRIBUTE,
                    SessionUser.from(authenticated));
        }

        Set<String> roleCodes = authenticated.roles().stream()
                .map(Role::code)
                .collect(Collectors.toSet());

        return Response.ok(new LoginResponse(authenticated.username(),
                authenticated.displayName(), roleCodes)).build();
    }

    @POST
    @Path("/logout")
    public Response logout()
    {
        HttpSession session = httpRequest != null ? httpRequest.getSession(false) : null;

        if (session != null)
        {
            session.invalidate();
        }

        return Response.ok(new LogoutResponse("Signed out")).build();
    }
}
