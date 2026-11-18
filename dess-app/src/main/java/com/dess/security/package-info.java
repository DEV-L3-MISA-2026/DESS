/**
 * @brief Authentication, authorization and user administration.
 *
 * <p>The module is split into a framework-independent domain, application services that
 * implement the use cases, infrastructure adapters for persistence, password hashing and
 * sessions, and a web delivery layer. Dependencies point inward: web and infrastructure may
 * depend on application and domain, while domain depends on neither.</p>
 *
 * <p>Other modules use the application services only. Domain types and infrastructure details
 * stay internal to the module.</p>
 */
package com.dess.security;
