package com.dess.platform;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Point d'entrée de l'API REST de l'application DESS.
 * 
 * Commentaire pour les développeurs :
 * Cette classe configure le chemin racine de tous les endpoints (/api)
 * et définit la documentation globale OpenAPI (Swagger).
 * WildFly génère automatiquement le document OpenAPI à partir de ces annotations.
 * 
 * Issue #63 : Compléter la documentation OpenAPI de l'API REST.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "DESS REST API",
        version = "0.1.0",
        description = "API de gestion académique pour un établissement (DESS)."
    ),
    servers = {
        @Server(url = "/dess-app/api", description = "Serveur local WildFly")
    },
    tags = {
        @Tag(name = "Health", description = "Vérification de l'état de fonctionnement du serveur"),
        @Tag(name = "Auth", description = "Authentification et gestion de session utilisateur")
    }
)
@ApplicationPath("/api")
public class DessApplication extends Application {
}
