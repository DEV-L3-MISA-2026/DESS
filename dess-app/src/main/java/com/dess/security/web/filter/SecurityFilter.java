package com.dess.security.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filtre de sécurité qui protège toutes les routes /api/*.
 *
 * Utilité : Empêche les utilisateurs non connectés d'accéder aux données.
 * Sans ce filtre, n'importe qui pourrait appeler n'importe quel endpoint.
 *
 * Fonctionnement :
 *  - Les routes publiques (/health, /auth/login) sont toujours accessibles
 *  - Pour le reste, on vérifie si la session HTTP contient un utilisateur connecté
 *  - Si non connecté → réponse 401 JSON avec un message d'erreur
 */
@WebFilter("/api/*")
public class SecurityFilter implements Filter {

    // Routes accessibles sans être connecté
    private static final String[] ROUTES_PUBLIQUES = {
        "/api/health",
        "/api/auth/login"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String chemin = request.getRequestURI();

        // Vérifier si la route est publique (pas besoin d'être connecté)
        if (isRoutePublic(chemin)) {
            chain.doFilter(req, res); // laisser passer
            return;
        }

        // Vérifier si l'utilisateur est connecté via sa session
        HttpSession session = request.getSession(false); // false = ne pas créer de nouvelle session
        boolean connecte = session != null && session.getAttribute("currentUser") != null;

        if (connecte) {
            chain.doFilter(req, res); // laisser passer
        } else {
            // Bloquer avec une erreur 401 en JSON
            envoyerErreur401(response);
        }
    }

    // Vérifie si l'URL correspond à une route publique
    // Extrait pour faciliter les tests unitaires
    public boolean isRoutePublic(String chemin) {
        for (String route : ROUTES_PUBLIQUES) {
            if (chemin != null && chemin.endsWith(route)) {
                return true;
            }
        }
        return false;
    }

    // Alias en français pour compatibilité avec le code de test existant
    public boolean estRoutePublique(String chemin) {
        return isRoutePublic(chemin);
    }

    // Envoie une réponse 401 en JSON
    // Extrait pour faciliter les tests unitaires
    public String construireJson401() {
        return "{\"code\":\"UNAUTHORIZED\",\"message\":\"Connexion requise\"}";
    }

    private void envoyerErreur401(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(construireJson401());
    }
}
