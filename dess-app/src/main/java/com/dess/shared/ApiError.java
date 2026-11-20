package com.dess.shared;

// Ity zany ny format d'erreur en generale ( Ho fafaina ity coms ity rehefa avy eo fa mba ho hitanlisany oe miasa tsara za)
/**
 * A simple error response returned by the REST API.
 *
 * @param code    a short machine-readable error code, e.g. "NOT_FOUND"
 * @param message a human-readable explanation of what went wrong
 */
public record ApiError(String code, String message) {}

