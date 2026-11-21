package com.dess.platform;

import com.dess.shared.ApiError;
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
 * Cette classe attrape automatiquement les exceptions et produit une réponse
 * JSON standardisée basée sur le record ApiError (code + message).
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    /**
     * Convertit une exception en objet ApiError standardisé.
     */
    public ApiError toApiError(Throwable exception) {
        String message = (exception != null && exception.getMessage() != null)
                ? exception.getMessage()
                : "Une erreur inattendue est survenue";

        return new ApiError("INTERNAL_ERROR", message);
    }

    @Override
    public Response toResponse(Throwable exception) {
        ApiError error = toApiError(exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
