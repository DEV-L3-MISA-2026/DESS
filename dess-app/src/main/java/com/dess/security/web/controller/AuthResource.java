package com.dess.security.web.controller;

import com.dess.security.application.AuthenticationService;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
import com.dess.shared.ApiError;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour l'authentification.
 *
 * Endpoint : POST /api/auth/login
 * Utilité : Permet à l'utilisateur ou à l'application frontend de se connecter.
 */
@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthenticationService authenticationService;

    // DTO simple pour la requête de connexion
    public record LoginRequest(String username, String password) {}

    // DTO simple pour la réponse en cas de succès
    public record LoginResponse(String username, String displayName, Set<String> roles) {}

    public AuthResource() {
    }

    // Constructeur pour faciliter les tests unitaires sans CDI
    public AuthResource(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("INVALID_INPUT", "Le nom d'utilisateur et le mot de passe sont requis"))
                    .build();
        }

        Optional<UserAccount> accountOpt = authenticationService.authenticate(
                request.username(),
                request.password()
        );

        if (accountOpt.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ApiError("UNAUTHORIZED", "Identifiants invalides"))
                    .build();
        }

        UserAccount account = accountOpt.get();
        Set<String> roleCodes = account.roles().stream()
                .map(Role::code)
                .collect(Collectors.toSet());

        LoginResponse response = new LoginResponse(
                account.username(),
                account.displayName(),
                roleCodes
        );

        return Response.ok(response).build();
    }
}
