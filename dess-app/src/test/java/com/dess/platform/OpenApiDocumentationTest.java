package com.dess.platform;

import com.dess.security.web.controller.AuthResource;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de validation de la documentation OpenAPI (Issue #63).
 * 
 * Commentaire pour les développeurs :
 * Ce test s'assure par réflexion que les annotations OpenAPI obligatoires
 * sont bien présentes sur l'application et sur les contrôleurs REST.
 */
class OpenApiDocumentationTest {

    @Test
    void dessApplication_a_une_definition_openapi() {
        OpenAPIDefinition definition = DessApplication.class.getAnnotation(OpenAPIDefinition.class);
        assertNotNull(definition, "DessApplication doit être annotée avec @OpenAPIDefinition");
        assertTrue(definition.info().title().contains("DESS"), "Le titre doit mentionner DESS");
    }

    @Test
    void healthResource_a_une_operation_documentee() throws NoSuchMethodException {
        Method checkMethod = HealthResource.class.getMethod("check");
        Operation op = checkMethod.getAnnotation(Operation.class);
        assertNotNull(op, "La méthode check() de HealthResource doit être documentée avec @Operation");
    }

    @Test
    void authResource_login_et_logout_sont_documentes() throws NoSuchMethodException {
        Method loginMethod = AuthResource.class.getMethod("login", AuthResource.LoginRequest.class);
        Operation loginOp = loginMethod.getAnnotation(Operation.class);
        assertNotNull(loginOp, "La méthode login() de AuthResource doit être documentée avec @Operation");

        Method logoutMethod = AuthResource.class.getMethod("logout");
        Operation logoutOp = logoutMethod.getAnnotation(Operation.class);
        assertNotNull(logoutOp, "La méthode logout() de AuthResource doit être documentée avec @Operation");
    }
}
