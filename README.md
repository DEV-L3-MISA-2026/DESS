# DESS — local-first backend scaffold

Maven multi-module Jakarta EE application with a graph-based JSP frontend for the institution
module.

```text
DESS/
├── pom.xml
├── dess-app/                  # WAR deployed to WildFly
│   ├── src/main/java/com/dess/
│   │   ├── institution/{domain,application,infrastructure}
│   │   ├── people/{domain,application,infrastructure}
│   │   ├── academic/{domain,application,infrastructure}
│   │   ├── assessment/{domain,application,infrastructure}
│   │   ├── scheduling/{domain,application,infrastructure}
│   │   ├── security/
│   │   └── shared/
│   └── src/main/webapp/       # Institution graph editor (index.jsp) and XML export
├── db-migration/              # Flyway migrations and Maven runner
├── ops/
│   ├── db/                    # Flyway helper script
│   ├── lib/                   # Shared shell environment loading
│   ├── postgres/              # Optional local PostgreSQL Compose file
│   └── wildfly/               # Driver, datasource, run and stop helpers
└── .env.example               # Local configuration template
```

## Stack
- Java 21; Jakarta EE 11 APIs; WildFly 42 runtime.
- Jakarta Persistence 3.2; persistence unit `dessPU`; Hibernate ORM is supplied by WildFly (do not bundle another Hibernate).
- PostgreSQL; WildFly JTA datasource `java:/jdbc/DessDS`.
- Flyway Maven plugin in `db-migration`; schema history is stored in `flyway_schema_history`.
- Maven multi-module build; JUnit 5.

## Prerequisites
- Java 21 and Maven 3.9+.
- WildFly 42.
- PostgreSQL 17, either installed locally or started with the bundled Compose file.
- Docker with Compose when using the bundled PostgreSQL container.

The WildFly `standalone/` directory must be writable by the user running the scripts. If `WILDFLY_HOME` points to a system installation owned by another user, copy it and use the copy:

```bash
cp -a /opt/wildfly-42.0.0.Final ~/wildfly-42
```

Then set `WILDFLY_HOME=$HOME/wildfly-42` in `.env`.

## Configuration
Copy the template and adjust the values. The real `.env` is ignored by Git.

```bash
cp .env.example .env
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `DESS_DB_HOST` | `localhost` | PostgreSQL host |
| `DESS_DB_PORT` | `5432` | PostgreSQL port |
| `DESS_DB_NAME` | `dess` | Database name |
| `DESS_DB_USER` | `dess` | Database user |
| `DESS_DB_PASSWORD` | none | Database password; required by migrations and datasource setup |
| `WILDFLY_HOME` | none | WildFly installation directory |
| `WILDFLY_PORT_OFFSET` | `0` | Shifts every WildFly port; `100` moves HTTP 8080→8180 and management 9990→10090 |
| `POSTGRES_JDBC_VERSION` | `42.7.7` | Optional JDBC driver override for the WildFly module installer |

The scripts load `.env` from the repository root. Variables already exported in the shell are used when `.env` does not define them.

## Quick start
```bash
cp .env.example .env
# edit DESS_DB_PASSWORD and WILDFLY_HOME in .env

./ops/wildfly/run.sh --with-db
```

If port 8080 is already used by another local service (for example Stremio), set a port offset
in `.env` and use the resulting URL:

```bash
WILDFLY_PORT_OFFSET=100
```

```bash
./ops/wildfly/run.sh --with-db
# application: http://localhost:8180/dess-app/
```

`run.sh` performs the following steps:
1. Starts the local PostgreSQL container when `--with-db` is used and the database host is local.
2. Builds `dess-app/target/dess-app.war`.
3. Applies pending Flyway migrations.
4. Installs the PostgreSQL JDBC module in WildFly when needed.
5. Starts WildFly in the background when it is not already running.
6. Configures the `java:/jdbc/DessDS` datasource and tests it.
7. Deploys the WAR with `deploy --force`.

The application is then available at <http://localhost:8080/dess-app/> (port 8080 plus
`WILDFLY_PORT_OFFSET`, so <http://localhost:8180/dess-app/> with offset `100`). The root URL
serves the institution test page described below.

Stop WildFly and the local database:

```bash
./ops/wildfly/stop.sh --with-db
```

Omit `--with-db` when PostgreSQL is external.

### Run options
| Option | Effect |
| --- | --- |
| `--with-db` | Start the PostgreSQL Compose container when `DESS_DB_HOST` is `localhost` or `127.0.0.1` |
| `--skip-build` | Reuse the existing WAR |
| `--skip-migrate` | Deploy without running Flyway |
| `-h`, `--help` | Show usage |

WildFly runs in the background; its console output is written to `.run/wildfly.out` and its server log remains in `$WILDFLY_HOME/standalone/log/server.log`.

## Institution tree frontend
<http://localhost:8080/dess-app/> is a JSP graph editor that exercises the institution module
through its CDI services. It is a development tool, not a production interface.

The page supports:

- creating a named tree (code, name and description), or copying one of the bundled XML examples
  from the WAR classpath to the configuration file;
- displaying the hierarchy as an interactive SVG canvas with one node per organizational unit;
- clicking a node to edit its code, name, description and custom attribute values in the side panel;
- dragging nodes to arrange the canvas, with the layout kept in browser local storage and a
  `Tidy layout` button that restores the automatic tree;
- moving a node with the parent selector or by Shift-dragging it onto another node;
- adding a child through the `+` handle on a node, or a root through `New root`; both open a dialog
  that only offers active unit types allowed by the configured parent-child relations;
- activating units, and deactivating them together with their descendants;
- deleting leaf units, unit types and allowed relations;
- reloading the XML file, checking the complete configuration, previewing the XML, and downloading
  the generated XML as `CODE.xml`.

Every graph edit, form submission and drag-and-drop move goes through the institution services. The
module validates the complete resulting configuration, including allowed relations, cycles,
required attributes and activation consistency, before persisting it. A rejected edit is reported
at the top of the page and leaves the previous configuration untouched. Unit types and tree settings
live in collapsible sections so the graph and the selected node stay in view.

The page has no authentication or CSRF protection. It is intended for a local WildFly instance
bound to localhost and must not be exposed on a network.

The XML file path is resolved in this order:

1. system property `dess.institution.config`;
2. environment variable `DESS_INSTITUTION_CONFIG`;
3. default `config/institution.xml`, relative to the WildFly working directory.

When WildFly is started with `ops/wildfly/run.sh` from the repository root, the default file is
therefore `config/institution.xml` at the repository root. The page displays the resolved absolute
path. Example files remain on the classpath under `dess-app/src/main/resources/examples/` and are
copied to that path only when the module is not initialized. Every write is validated, persisted
atomically and preceded by a `.bak` copy of the previous file.

The frontend is a JSP on purpose: the institution architecture guard forbids servlet and JPA imports
in `src/main/java`. `index.jsp` calls the CDI producers of the module and never uses the XML
infrastructure directly. `export.jsp` streams the persisted configuration file as an attachment;
the tree code is used as the download name when a configuration is loaded.

## Database migrations
Run Flyway at any time:

```bash
./ops/db/migrate.sh            # print migration status, then apply pending migrations
./ops/db/migrate.sh info       # status only
./ops/db/migrate.sh validate   # check applied migrations
./ops/db/migrate.sh repair     # repair the schema history after a failed migration
```

Extra arguments are passed to Maven, for example `./ops/db/migrate.sh migrate -Dflyway.target=2`.

Manage the optional PostgreSQL container directly:

```bash
docker compose --env-file .env -f ops/postgres/compose.yaml up -d
docker compose --env-file .env -f ops/postgres/compose.yaml stop
```

The container stores data in the `dess-postgres-data` volume. `stop` preserves it; `down` also preserves the named volume unless it is removed explicitly.

Both `ops/db/migrate.sh` and `ops/wildfly/run.sh` check that a local `DESS_DB_PORT` accepts
connections before Maven or WildFly starts. When the container is healthy but its published port
is unreachable, the check prints the repair command, usually
`docker network connect postgres_default dess-postgres`. Set `DESS_SKIP_DB_PORT_CHECK=1` to skip
the check when the port is reachable from Java but not from Bash.

## Persistence
`dess-app/src/main/resources/META-INF/persistence.xml` declares the JTA persistence unit `dessPU`:

- datasource `java:/jdbc/DessDS`;
- Hibernate ORM supplied by WildFly;
- schema generation disabled through `jakarta.persistence.schema-generation.database.action=none` and `hibernate.hbm2ddl.auto=none`.

No JPA entities are declared yet. Flyway remains the only component that changes the database schema.

## Migration policy
Put SQL files in `db-migration/src/main/resources/db/migration/` using names such as `V1__initial_schema.sql`. Never edit a migration already applied to a shared database; add a new version. Run Flyway before deploying the corresponding application version. Flyway owns schema evolution; Hibernate must not create or update the schema.

## Boundaries
- Modules call other modules through their `application` layer, not by reaching into their repositories.
- `academic` owns regular teaching sessions; `assessment` owns exam sessions; `scheduling` owns rooms, bookings and conflict detection. Assessment and academic call scheduling to reserve resources.
- `shared` stays small and technical.
- One local WildFly instance and one local PostgreSQL database for this development setup. No cloud deployment, tenant routing, orchestration or production frontend.

For production, do not store datasource passwords as plain text in CLI scripts; use a WildFly credential store or deployment secret management.
