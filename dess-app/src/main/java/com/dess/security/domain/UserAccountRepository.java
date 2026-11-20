package com.dess.security.domain;

import java.util.Optional;

/**
 * Interface pour la persistance des comptes utilisateurs.
 *
 * Utilité : Permet à AuthenticationService de charger un compte par son username
 * sans dépendre directement d'une base de données SQL ou d'une technologie particulière.
 */
public interface UserAccountRepository {

    /**
     * Recherche un compte utilisateur par son identifiant de connexion (username).
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Sauvegarde ou met à jour un compte utilisateur.
     */
    void save(UserAccount account);
}
