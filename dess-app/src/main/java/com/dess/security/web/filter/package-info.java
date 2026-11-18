/**
 * @brief Servlet filters guarding web resources.
 *
 * <p>Filters establish the current user from the session and reject unauthenticated or
 * unauthorized requests before they reach a controller or JSP. They delegate each decision to
 * the authorization service.</p>
 */
package com.dess.security.web.filter;
