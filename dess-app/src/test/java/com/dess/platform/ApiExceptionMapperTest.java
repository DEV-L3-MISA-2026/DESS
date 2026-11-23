package com.dess.platform;

import com.dess.shared.ApiError;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests unitaires pour ApiExceptionMapper (Issue #62).
 * Vérifie que chaque type d'exception est correctement traduit en ApiError et code HTTP.
 */
class ApiExceptionMapperTest {

    private ApiExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ApiExceptionMapper();
    }

    @Test
    void mapsGenericExceptionToInternalError() {
        RuntimeException exception = new RuntimeException("Erreur inattendue");

        ApiError error = mapper.toApiError(exception);
        int status = mapper.toStatusCode(exception);

        assertNotNull(error);
        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("Erreur inattendue", error.message());
        assertEquals(500, status);
    }

    @Test
    void mapsIllegalArgumentExceptionToInvalidInput() {
        IllegalArgumentException exception = new IllegalArgumentException("Identifiant invalide");

        ApiError error = mapper.toApiError(exception);
        int status = mapper.toStatusCode(exception);

        assertNotNull(error);
        assertEquals("INVALID_INPUT", error.code());
        assertEquals("Identifiant invalide", error.message());
        assertEquals(400, status);
    }

    @Test
    void mapsWebApplicationExceptionWithStatus() {
        // En passant notre propre objet Response, on évite d'appeler Response.status()
        // qui nécessite l'implémentation JAX-RS de WildFly hors ligne.
        Response dummyResponse = new SimpleTestResponse(404);
        WebApplicationException exception = new WebApplicationException("Ressource introuvable", dummyResponse);

        ApiError error = mapper.toApiError(exception);
        int status = mapper.toStatusCode(exception);

        assertNotNull(error);
        assertEquals("NOT_FOUND", error.code());
        assertEquals("Ressource introuvable", error.message());
        assertEquals(404, status);
    }

    @Test
    void handlesNullMessageGracefully() {
        NullPointerException exception = new NullPointerException();

        ApiError error = mapper.toApiError(exception);
        int status = mapper.toStatusCode(exception);

        assertNotNull(error);
        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("Une erreur inattendue est survenue", error.message());
        assertEquals(500, status);
    }

    @Test
    void handlesNullThrowableGracefully() {
        ApiError error = mapper.toApiError(null);
        int status = mapper.toStatusCode(null);

        assertNotNull(error);
        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals(500, status);
    }

    /**
     * Implémentation minimale de Response pour les tests sans conteneur WildFly.
     */
    private static class SimpleTestResponse extends Response {
        private final int status;

        SimpleTestResponse(int status) {
            this.status = status;
        }

        @Override
        public int getStatus() { return status; }
        @Override
        public StatusType getStatusInfo() { return null; }
        @Override
        public Object getEntity() { return null; }
        @Override
        public <T> T readEntity(Class<T> entityType) { return null; }
        @Override
        public <T> T readEntity(GenericType<T> entityType) { return null; }
        @Override
        public <T> T readEntity(Class<T> entityType, Annotation[] annotations) { return null; }
        @Override
        public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) { return null; }
        @Override
        public boolean hasEntity() { return false; }
        @Override
        public boolean bufferEntity() { return false; }
        @Override
        public void close() {}
        @Override
        public MediaType getMediaType() { return null; }
        @Override
        public Locale getLanguage() { return null; }
        @Override
        public int getLength() { return 0; }
        @Override
        public Set<String> getAllowedMethods() { return null; }
        @Override
        public Map<String, NewCookie> getCookies() { return null; }
        @Override
        public EntityTag getEntityTag() { return null; }
        @Override
        public Date getDate() { return null; }
        @Override
        public Date getLastModified() { return null; }
        @Override
        public URI getLocation() { return null; }
        @Override
        public Set<Link> getLinks() { return null; }
        @Override
        public boolean hasLink(String relation) { return false; }
        @Override
        public Link getLink(String relation) { return null; }
        @Override
        public Link.Builder getLinkBuilder(String relation) { return null; }
        @Override
        public MultivaluedMap<String, Object> getMetadata() { return null; }
        @Override
        public MultivaluedMap<String, String> getStringHeaders() { return null; }
        @Override
        public String getHeaderString(String name) { return null; }
    }
}
