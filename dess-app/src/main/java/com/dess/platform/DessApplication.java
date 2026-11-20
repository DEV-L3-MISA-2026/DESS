package com.dess.platform;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Entry point for the REST API.
 * All endpoints are available under /api.
 * OpenAPI documentation is generated automatically by WildFly.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "DESS API",
        version = "0.1.0",
        description = "Academic management system for one institution"
    )
)
@ApplicationPath("/api")
public class DessApplication extends Application {}

