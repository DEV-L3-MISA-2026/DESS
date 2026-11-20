package com.dess.platform;

import com.dess.shared.ApiError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test unitaire pour ApiExceptionMapper.
 * Vérifie que les exceptions sont correctement converties en objet ApiError.
 */
class ApiExceptionMapperTest {

    @Test
    void mapsExceptionToInternalError() {
        ApiExceptionMapper mapper = new ApiExceptionMapper();
        RuntimeException exception = new RuntimeException("Erreur de test");

        ApiError error = mapper.toApiError(exception);

        assertNotNull(error);
        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("Erreur de test", error.message());
    }

    @Test
    void handlesNullMessageGracefully() {
        ApiExceptionMapper mapper = new ApiExceptionMapper();
        NullPointerException exception = new NullPointerException();

        ApiError error = mapper.toApiError(exception);

        assertNotNull(error);
        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("Une erreur inattendue est survenue", error.message());
    }
}
