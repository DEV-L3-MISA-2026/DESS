# Instructions pour l'Agent

## Conventions de langue

Tu dois impérativement respecter les règles de nommage et de documentation suivantes pour tous les fichiers de code que tu génères ou modifies :

### 1. Commentaires

- **Langue obligatoire :** Français (`fr`).
- **Périmètre :** Tous les commentaires (lignes, blocs, JSDoc, docstrings) expliquant le code ou la logique doivent être rédigés en français.

### 2. Noms de variables et identifiants

- **Langue obligatoire :** Anglais (`en`).
- **Périmètre :**
  - Noms de variables, constantes et arguments.
  - Noms de fonctions, méthodes et classes.
  - Noms de tables, colonnes, alias et bases de données dans les requêtes SQL.
  - Routes d'API, noms de fichiers de code et clés de configuration.

> La syntaxe du langage et ses mots-clés réservés restent inchangés.

## Objectif fonctionnel du projet

DESS est une application JEE locale destinée à gérer :

- Les emplois du temps de cours, TD, TP, examens et rattrapages.
- Les parcours, niveaux, UE, ECUE, inscriptions et trimestres.
- Les étudiants, enseignants, administrateurs et leurs droits.
- Les salles, réservations, disponibilités et conflits de planning.
- Les absences des enseignants.
- Les notes, résultats d'examens et inscription au trimestre suivant.
- Les notifications par e-mail et les modifications d'emploi du temps.
- Un tableau de bord administratif.

Rôles fonctionnels attendus :

- **Étudiant** : consulter son emploi du temps et ses résultats.
- **Enseignant** : réserver ou annuler des créneaux, déclarer une absence, définir un examen ou un rattrapage, importer des notes et recevoir les feuilles de résultats.
- **Administrateur** : gérer les utilisateurs, les salles, les configurations et les activités globales.

## Stack technique imposée

- Java 21.
- Maven avec des modules parents et enfants.
- Jakarta EE 11.
- CDI 4.1.
- WildFly 42 comme serveur d'application.
- Application Java empaquetée en fichier WAR `dess-app.war`.
- PostgreSQL comme base de données principale.
- Datasource WildFly JTA nommée `java:/jdbc/DessDS`.
- Hibernate fourni par WildFly : aucune dépendance Hibernate ne doit être embarquée.
- Flyway pour toutes les évolutions du schéma PostgreSQL.
- JUnit 5 pour les tests unitaires et d'intégration.
- Encodage source UTF-8.

La documentation locale retenue doit rester cohérente avec `README.md` et les fichiers POM. Certaines anciennes références à Tomcat, JBoss ou JSTL ne remplacent pas la stack cible actuelle.

## Architecture modulaire

Le projet est organisé autour des modules suivants :

- `institution` : établissement, types d'unités, hiérarchie organisationnelle et attributs dynamiques.
- `people` : utilisateurs, étudiants et enseignants.
- `academic` : cours, UE, ECUE et sessions pédagogiques ordinaires.
- `assessment` : examens, rattrapages et résultats.
- `scheduling` : salles, réservations et détection de conflits.
- `security` : authentification, autorisation et rôles.
- `shared` : éléments techniques communs réduits au strict nécessaire.

Règles architecturales obligatoires :

- Un module doit être appelé par les autres modules uniquement via sa couche `application`.
- Ne pas accéder directement aux repositories ou classes d'infrastructure d'un autre module.
- Les entités métier ne doivent pas utiliser directement JPA, SQL, REST, Servlet ou Hibernate.
- Ne pas utiliser JPA ou REST dans le module `institution` tant qu'ils n'ont pas été explicitement requis pour une fonctionnalité transversale.
- `academic` gère les sessions pédagogiques régulières.
- `assessment` gère les sessions d'examen.
- `scheduling` possède les salles, réservations et règles de conflit.
- Les modules `academic` et `assessment` utilisent `scheduling` pour réserver les ressources.
- Toute nouvelle dépendance entre modules doit respecter ces frontières.

## Persistance et migrations

- PostgreSQL est la base de données cible.
- Flyway est le propriétaire exclusif des évolutions de schéma.
- Hibernate ne doit jamais créer, mettre à jour ou supprimer des tables.
- Les migrations se trouvent dans `db-migration/src/main/resources/db/migration/`.
- Les fichiers SQL doivent suivre le format `V<n>__<description>.sql`.
- Une migration appliquée ne doit jamais être modifiée : toute correction nécessite une nouvelle version.
- Exécuter les migrations avant le déploiement de la version applicative correspondante.
- `flyway_schema_history` conserve l'historique Flyway.
- Le nettoyage automatique doit rester désactivé.
- Utiliser les variables d'environnement `DB_URL`, `DB_USER` et `DB_PASSWORD` pour les migrations.
- Utiliser les variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` et `DB_PASSWORD` pour l'environnement local.
- Ne pas exposer ou versionner les mots de passe réels.

## Configuration du module institution

- Les types d'unités organisationnelles ne doivent pas être codés en dur.
- Les relations parent-enfant doivent être configurables.
- Les identifiants des unités organisationnelles sont stables et peuvent servir de clés étrangères dans d'autres modules.
- Les objets métier retournés doivent être immuables et non modifiables par l'appelant.
- Toute modification doit produire un nouvel état complet validé avant persistance.
- Les écritures doivent être sérialisées dans une JVM avec verrouillage explicite.
- Le verrouillage local ne garantit pas la cohérence entre plusieurs JVM utilisant le même fichier XML.
- Le chemin du fichier XML doit être déterminé dans cet ordre : propriété système `dess.institution.config`, variable d'environnement `DESS_INSTITUTION_CONFIG`, puis `config/institution.xml`.

## Contraintes XML

- Les configurations XML doivent respecter `dess-app/src/main/resources/institution.xsd`.
- Valider d'abord la syntaxe XML, puis le XSD, puis les règles métier.
- Les codes doivent correspondre à `[A-Za-z0-9][A-Za-z0-9_.\\-]*`.
- Les clés d'attribut commencent par une lettre, puis peuvent contenir des lettres, chiffres ou `_`.
- Les clés d'attribut doivent être uniques dans leur type.
- Les types d'attribut ne doivent être ni codés en dur ni définis sous forme d'énumérations de types d'unités métier.
- Les attributs obligatoires, les valeurs par défaut et les types de données doivent être validés.
- Les relations parent-enfant interdites doivent être rejetées.
- Les entités DOCTYPE, entités externes et accès réseau sont interdits pour empêcher les attaques XXE.
- Une configuration invalide ne doit jamais écraser un fichier valide existant.
- Toute sauvegarde XML doit utiliser un fichier temporaire du même dossier, être relue puis remplacée atomiquement.
- Conserver une copie de sauvegarde `<fichier>.bak` avant le remplacement réussi.

## Sécurité et identifiants

- Prévoir une authentification OAuth avec authentification à deux facteurs.
- Ne pas stocker de mot de passe utilisateur localement tant qu'un fournisseur OAuth n'est pas explicitement configuré.
- Chaque accès sensible doit vérifier le rôle de l'utilisateur.
- Les résultats d'examen ne doivent être consultés que par les personnes autorisées.
- Ne pas conserver les mots de passe PostgreSQL en clair dans les scripts ou le dépôt en production.
- Utiliser un credential store WildFly ou un secret manager en production.
- Activer les garde-fous XML contre les entités externes et les DTD.

## Tests et validation

- Tous les nouveaux comportements doivent avoir des tests JUnit 5 adaptés.
- Conserver les tests d'architecture empêchant les importations SQL, JPA, REST, Hibernate, Servlet ou de modules voisins dans les couches interdites.
- Vérifier les fichiers XML valides et invalides avec des tests dédiés.
- Vérifier les erreurs de XSD, XML malformé, XXE, doublons, hiérarchie interdite et attributs invalides.
- Vérifier que les structures retournées sont immuables lorsqu'elles sont conçues comme telles.
- Une modification applicative doit être validée avant d'être persévérée.
- Exécuter au minimum `mvn clean verify` avant de considérer une modification terminée.
- Ne pas déclarer une fonctionnalité terminée si les tests échouent.

## Déploiement local

- PostgreSQL peut être démarré avec `ops/postgres/compose.yaml`.
- `WILDFLY_HOME` doit désigner l'installation WildFly locale.
- Installer le pilote PostgreSQL avec `ops/wildfly/install-postgresql-driver.sh`.
- Configurer la datasource avec `ops/wildfly/configure-datasource.sh`.
- Exécuter `mvn -pl db-migration flyway:info` puis `mvn -pl db-migration flyway:migrate`.
- Construire avec `mvn clean verify`.
- Déployer `dess-app/target/dess-app.war` dans WildFly.

## Périmètre actuel

- Aucun frontend complet n'est actuellement inclus.
- OAuth 2FA, notifications, API REST, sécurité fine et JPA applicatif restent à finaliser.
- Les tables métier PostgreSQL ne sont pas encore définies : seuls les premiers fichiers de migration existent.
- Le module `institution` est actuellement l'implémentation de référence pour le domaine, l'application, l'infrastructure, l'immuabilité et la validation.
- Les anciennes maquettes ou notes de conception ne doivent pas remplacer les conventions techniques ci-dessus lorsqu'elles sont en conflit avec elles.