package com.dess.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

/**
 * Endpoint de santé (Health check) de l'application.
 * 
 * Accessible sur : GET /api/health
 * Utilité : Permet à Mikajy (DevOps), aux autres membres de l'équipe
 * et aux outils de surveillance de vérifier que l'application répond bien.
 * 
 * Commentaire pour les développeurs :
 * Les annotations @Tag, @Operation et @APIResponse permettent à OpenAPI
 * de documenter cet endpoint pour Swagger UI et pour les clients de l'API.
 */
@Path("/health")
@Tag(name = "Health")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Vérifier la santé du serveur",
        description = "Retourne l'état UP pour confirmer que l'application et le serveur WildFly tournent correctement."
    )
    @APIResponse(
        responseCode = "200",
        description = "Le serveur fonctionne correctement",
        content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Map<String, String> check() {
        return Map.of("status", "UP");
    }
}
