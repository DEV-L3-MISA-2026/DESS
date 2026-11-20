package com.dess.shared;

/**
 * A simple error response returned by the REST API.
 *
 * @param code    a short machine-readable error code, e.g. "NOT_FOUND"
 * @param message a human-readable explanation of what went wrong
 */
public record ApiError(String code, String message) {}
