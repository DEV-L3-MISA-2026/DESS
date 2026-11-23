package com.dess.security.web.controller;

import com.dess.security.application.AuthenticationService;
import com.dess.security.domain.Role;
import com.dess.security.domain.UserAccount;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour l'authentification.
 *
 * Endpoints :
 *   POST /api/auth/login  — connexion (crée une session HTTP)
 *   POST /api/auth/logout — déconnexion (invalide la session)
 *
 * Utilité : Permet à l'utilisateur de se connecter et de se déconnecter.
 * La session HTTP retournée (cookie) est ensuite vérifiée par SecurityFilter.
 *
 * Commentaire pour les développeurs :
 * Les annotations @Tag, @Operation, @APIResponse et @Schema permettent de documenter
 * précisément ces endpoints dans OpenAPI (Swagger) pour les développeurs du frontend.
 */
@Path("/auth")
@Tag(name = "Auth", description = "Authentification et gestion de session utilisateur")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthenticationService authenticationService;

    // La requête HTTP courante, injectée par JAX-RS
    // Utilisée pour accéder à la session HTTP
    @Context
    private HttpServletRequest httpRequest;

    // DTO simple pour la requête de connexion avec documentation OpenAPI
    @Schema(description = "Identifiants de connexion")
    public record LoginRequest(
        @Schema(description = "Nom d'utilisateur", example = "admin", required = true)
        String username,
        @Schema(description = "Mot de passe", example = "admin123", required = true)
        String password
    ) {}

    // DTO simple pour la réponse en cas de succès avec documentation OpenAPI
    @Schema(description = "Informations sur le compte connecté")
    public record LoginResponse(
        @Schema(description = "Nom d'utilisateur", example = "admin")
        String username,
        @Schema(description = "Nom affiché", example = "Administrateur")
        String displayName,
        @Schema(description = "Liste des codes de rôles", example = "[\"ADMIN\"]")
        Set<String> roles
    ) {}

    public AuthResource() {
    }

    // Constructeur pour faciliter les tests unitaires sans CDI
    public AuthResource(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    // Constructeur complet pour les tests (avec session HTTP mockée)
    public AuthResource(AuthenticationService authenticationService, HttpServletRequest httpRequest) {
        this.authenticationService = authenticationService;
        this.httpRequest = httpRequest;
    }

    @POST
    @Path("/login")
    @Operation(
        summary = "Connexion utilisateur",
        description = "Authentifie un utilisateur avec son nom et mot de passe, et crée une session HTTP."
    )
    @APIResponse(
        responseCode = "200",
        description = "Authentification réussie",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = LoginResponse.class))
    )
    @APIResponse(
        responseCode = "400",
        description = "Nom d'utilisateur ou mot de passe manquant",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))
    )
    @APIResponse(
        responseCode = "401",
        description = "Identifiants invalides",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))
    )
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

        // Stocker l'utilisateur dans la session HTTP
        // true = créer une session si elle n'existe pas encore
        if (httpRequest != null) {
            httpRequest.getSession(true).setAttribute("currentUser", account);
        }

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

    @POST
    @Path("/logout")
    @Operation(
        summary = "Déconnexion utilisateur",
        description = "Invalide la session HTTP de l'utilisateur actuellement connecté."
    )
    @APIResponse(
        responseCode = "200",
        description = "Déconnexion réussie"
    )
    public Response logout() {
        // Récupérer la session existante sans en créer une nouvelle
        HttpSession session = httpRequest != null ? httpRequest.getSession(false) : null;

        if (session != null) {
            session.invalidate(); // Supprimer toutes les données de session
        }

        return Response.ok("{\"message\":\"Déconnexion réussie\"}").build();
    }
}
