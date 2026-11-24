package com.dess.platform;

import com.dess.shared.ApiError;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Intercepteur global d'erreurs pour l'API REST.
 *
 * Utilité pour l'équipe :
 * Par défaut, lorsqu'une erreur survient, WildFly renvoie une page d'erreur HTML.
 * Les clients de notre API (comme une application React ou Postman) ont besoin d'une réponse JSON.
 * Cette classe attrape automatiquement toutes les exceptions non capturées et produit
 * une réponse JSON standardisée basée sur le record ApiError (code + message).
 *
 * Commentaire pour les développeurs :
 * Issue #62 : Appliquer le modèle d'erreur REST à toutes les ressources.
 * On gère ici :
 *  - Les IllegalArgumentException -> HTTP 400 (INVALID_INPUT)
 *  - Les WebApplicationException (ex: 404 NotFound) -> vrai code HTTP et code textuel
 *  - Les autres exceptions -> HTTP 500 (INTERNAL_ERROR)
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    /**
     * Convertit une exception en objet ApiError standardisé (code + message).
     * Méthode purement Java, facile à tester sans conteneur d'injection ou JAX-RS.
     */
    public ApiError toApiError(Throwable exception) {
        if (exception == null) {
            return new ApiError("INTERNAL_ERROR", "Une erreur inattendue est survenue");
        }

        // Erreur d'argument invalide (ex: validation métier qui échoue)
        if (exception instanceof IllegalArgumentException) {
            String message = exception.getMessage() != null ? exception.getMessage() : "Paramètre invalide";
            return new ApiError("INVALID_INPUT", message);
        }

        // Exception HTTP standard JAX-RS (ex: 404 Not Found, 403 Forbidden)
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse() != null ? wae.getResponse().getStatus() : 500;
            String message = wae.getMessage() != null ? wae.getMessage() : "Erreur HTTP " + status;
            String code = switch (status) {
                case 400 -> "BAD_REQUEST";
                case 401 -> "UNAUTHORIZED";
                case 403 -> "FORBIDDEN";
                case 404 -> "NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                default -> "HTTP_ERROR_" + status;
            };
            return new ApiError(code, message);
        }

        // Toute autre exception inattendue
        String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
                ? exception.getMessage()
                : "Une erreur inattendue est survenue";

        return new ApiError("INTERNAL_ERROR", message);
    }

    /**
     * Détermine le code de statut HTTP adapté à l'exception.
     */
    public int toStatusCode(Throwable exception) {
        if (exception instanceof IllegalArgumentException) {
            return Response.Status.BAD_REQUEST.getStatusCode(); // 400
        }
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse() != null ? wae.getResponse().getStatus() : 500;
        }
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(); // 500
    }

    @Override
    public Response toResponse(Throwable exception) {
        ApiError error = toApiError(exception);
        int status = toStatusCode(exception);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
