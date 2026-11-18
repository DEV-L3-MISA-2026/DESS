/**
 * @brief Security use cases.
 *
 * <p>{@code AuthenticationService} verifies credentials, {@code AuthorizationService} answers
 * permission questions, and {@code UserManagementService} administers accounts and role
 * assignments. The services orchestrate the domain model and call infrastructure through
 * ports; they do not depend on servlets, JPA or concrete storage.</p>
 */
package com.dess.security.application;
