package com.dess.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Endpoint de santé (Health check) de l'application.
 * 
 * Accessible sur : GET /api/health
 * Utilité : Permet à Mikajy (DevOps) et aux outils de surveillance
 * de vérifier simplement que l'application tourne et répond (status = UP).
 */
@Path("/health")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> check() {
        return Map.of("status", "UP");
    }
}

