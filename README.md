# DESS - academic management app

DESS manages the academic activities of one institution. It is a Jakarta EE application that
runs locally on WildFly with PostgreSQL. The institution structure is stored in XML. The current
frontend is a set of JSP pages: an authenticated application shell with working people, academic,
scheduling and security pages, plus the institution graph editor.

The team plan targets a REST API and a React frontend. This repository does not have them yet.
The plan is in [docs/PMP.md](docs/PMP.md); the architecture summary is in
[docs/architecture_farany.md](docs/architecture_farany.md). The UI/UX baseline is in
[docs/design/](docs/design/).

## Current status

| Area | State |
| --- | --- |
| Institution module | Working: XML config, validation, graph editor and export. |
| Security module | Working for local use: PBKDF2 passwords, login, roles, CSRF checks and request filtering. Accounts are held in memory. |
| People, academic, scheduling | Working through JSP pages: lists and create forms backed by JPA. |
| Assessment | Package placeholder. |
| REST API | Health endpoint only; domain endpoints are not implemented. |
| JPA entities | Implemented for people, academic and scheduling. Flyway owns the schema. |
| Frontend | JSP pages under `WEB-INF/views/` plus the institution editor. React is a target, not current code. |
| Design system | Baseline in `docs/design/`; implemented on the application pages and the institution page. |

## Documentation

| File | What it covers |
| --- | --- |
| [COLLABORATORS.md](COLLABORATORS.md) | Team, ownership, branches and working rules. |
| [AGENTS.md](AGENTS.md) | Rules for AI coding agents. |
| [docs/security-module.md](docs/security-module.md) | Security module layout and current gaps. |
| [docs/institution-dependency-graph.md](docs/institution-dependency-graph.md) | Institution module dependencies. |
| [docs/PMP.md](docs/PMP.md) | Two-week MVP plan. |
| [docs/architecture_farany.md](docs/architecture_farany.md) | Architecture summary. |
| [docs/design/](docs/design/) | UI/UX design system, tokens, components, page specifications and UX rules. |

## Repository layout

```text
DESS/
├── COLLABORATORS.md           # Team and branch rules
├── AGENTS.md                  # Guide for AI agents
├── README.md
├── dess-app/                  # WAR deployed to WildFly
│   ├── src/main/java/com/dess/
│   │   ├── institution/       # XML configuration, validation and graph
│   │   ├── security/          # Accounts, roles, filters and servlets
│   │   ├── people/            # JPA entities and services
│   │   ├── academic/          # JPA entities and services
│   │   ├── assessment/        # Package placeholder
│   │   ├── scheduling/        # JPA entities and services
│   │   └── shared/
│   └── src/main/webapp/
│       ├── index.jsp          # Institution graph editor
│       ├── export.jsp         # Configuration download
│       └── WEB-INF/views/     # Application pages and shared fragments
├── db-migration/              # Flyway migrations
├── docs/                      # Architecture, module notes and design system
│   └── design/                # UI/UX baseline
├── ops/
│   ├── db/                    # Flyway helper
│   ├── lib/                   # Shared shell helpers
│   ├── postgres/              # Local PostgreSQL Compose file
│   └── wildfly/               # WildFly run and stop scripts
└── .env.example               # Local configuration template
```

The `.agents/` and `.opencode/agents/` directories hold local agent skills and subagents. They
are ignored by Git and do not ship with the repository.

## Stack

- Java 21 and Jakarta EE 11.
- WildFly 42.
- CDI for dependency injection.
- JSP with JSTL and Expression Language for the current frontend.
- Bootstrap 5, custom CSS and vanilla JavaScript for the design baseline.
- PostgreSQL for operational data.
- Flyway for database migrations.
- JUnit 5 for tests.
- Maven for the build.

JPA is configured and used by the people, academic and scheduling modules. Hibernate is
supplied by WildFly. Do not bundle a second Hibernate version.

## Prerequisites

- Java 21 and Maven 3.9 or newer.
- WildFly 42.
- PostgreSQL 17, either installed locally or started with the bundled Compose file.
- Docker with Compose when you use the bundled PostgreSQL container.

The WildFly `standalone/` directory must be writable by the user who runs the scripts. If
`WILDFLY_HOME` points to a system installation owned by another user, copy it and use the copy:

```bash
cp -a /opt/wildfly-42.0.0.Final ~/wildfly-42
```

Then set `WILDFLY_HOME=$HOME/wildfly-42` in `.env`.

## Configuration

Copy the template and edit the values. The real `.env` is ignored by Git.

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
| `WILDFLY_PORT_OFFSET` | `0` | Shifts every WildFly port; `100` moves HTTP 8080 to 8180 and management 9990 to 10090 |
| `POSTGRES_JDBC_VERSION` | `42.7.7` | Optional JDBC driver version for the WildFly module installer |
| `DESS_ADMIN_PASSWORD` | none | Password of the initial `admin` account. No account is created when it is unset |
| `DESS_PROFESSOR_PASSWORD` | none | Password of the initial `professor` account. No account is created when it is unset |
| `DESS_INSTITUTION_CONFIG` | `config/institution.xml` | Institution XML file used by the graph editor |

Initial accounts live in memory. They are recreated on every restart, and accounts created from
the security page are lost when the application restarts.

The scripts load `.env` from the repository root. Variables already exported in the shell are
used when `.env` does not define them.

## Quick start

```bash
cp .env.example .env
# edit DESS_DB_PASSWORD and WILDFLY_HOME in .env

./ops/wildfly/run.sh --with-db
```

If port 8080 is already used by another local service, set a port offset in `.env`:

```bash
WILDFLY_PORT_OFFSET=100
```

Then run the script again:

```bash
./ops/wildfly/run.sh --with-db
# application: http://localhost:8180/dess-app/
```

`run.sh` does these steps:

1. Starts the local PostgreSQL container when `--with-db` is used and the database host is local.
2. Builds `dess-app/target/dess-app.war`.
3. Applies pending Flyway migrations.
4. Installs the PostgreSQL JDBC module in WildFly when needed.
5. Starts WildFly in the background when it is not already running.
6. Configures the `java:/jdbc/DessDS` datasource and tests it.
7. Deploys the WAR with `deploy --force`.

The application is available at <http://localhost:8080/dess-app/> (port 8080 plus
`WILDFLY_PORT_OFFSET`). The root URL redirects to the sign-in page; after signing in, the
overview page is at `/app` and the institution graph editor is at `/`.

Sign in with `admin` or `professor` and the passwords you put in `.env`. When no password is
configured, the login page explains which variables to set.

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

WildFly runs in the background. Its console output goes to `.run/wildfly.out`; the server log
stays in `$WILDFLY_HOME/standalone/log/server.log`.

## Application pages and access control

Every page except the login page, the error pages and `/api/health` requires a session. The
shared shell is made of JSP fragments under `WEB-INF/views/components/`, and each page declares
the JSTL core library once.

| Route | Page | Who may open it |
| --- | --- | --- |
| `/login` | Sign in | Everyone |
| `/logout` | Ends the session (POST with a CSRF token) | Signed-in users |
| `/app` | Overview with counts | Signed-in users |
| `/app/people` | Student and teacher lists, create form | Signed-in users; only administrators can create |
| `/app/academic` | Periods, programs and courses | Signed-in users; only administrators can create |
| `/app/scheduling` | Rooms and bookings | Signed-in users; only administrators can create |
| `/app/security` | Account management | Administrators only |
| `/api/health` | Health check | Everyone |
| `/api/auth/login` | JSON login | Everyone |
| `/api/*` other paths | JSON API | Signed-in users |

Behaviour:

- Passwords are stored as PBKDF2 hashes with a per-account salt. The session holds the account
  id, username, display name and roles, never the password hash.
- `SecurityFilter` redirects anonymous page requests to `/login` and keeps the original path in
  `next`. API requests receive a JSON `401` instead of a redirect.
- `CsrfFilter` runs before `SecurityFilter` and rejects any POST without a valid token with
  `403`. Every POST form in the application carries `csrfToken`.
- The session id is changed after sign-in, cookies are HTTP-only and the session expires after
  30 minutes.
- The sidebar hides the administration section from professors. That is navigation only; the
  server enforces the same rule on every request.
- A failed sign-in returns `401` and a generic message. Five failures in five minutes lock that
  username and client address for five minutes.

## Institution graph editor

<http://localhost:8080/dess-app/> is a JSP graph editor for the institution module. It is a
development tool, not a production interface.

The page lets you:

- create a named tree, or copy one of the bundled XML examples;
- see the hierarchy as an interactive SVG canvas;
- click a node to edit its code, name, description and custom attribute values;
- drag nodes to arrange the canvas, or press `Tidy layout` to restore the automatic tree;
- move a node with the parent selector or by Shift-dragging it onto another node;
- add a child with the `+` handle, or a root with `New root`;
- activate units, or deactivate them together with their descendants;
- delete leaf units, unit types and allowed relations;
- reload the XML file, check the configuration, preview the XML and download it as `CODE.xml`.

Every edit goes through the institution services. The module validates the complete
configuration, including allowed relations, cycles, required attributes and activation rules,
before it saves. A rejected edit is shown at the top of the page and leaves the old
configuration untouched.

The page requires a signed-in session and every POST form carries a CSRF token. It still
assumes a local WildFly instance bound to localhost. Do not expose it on a network.

The XML file path is resolved in this order:

1. system property `dess.institution.config`;
2. environment variable `DESS_INSTITUTION_CONFIG`;
3. default `config/institution.xml`, relative to the WildFly working directory.

When WildFly is started with `ops/wildfly/run.sh` from the repository root, the default file is
`config/institution.xml` at the repository root. The page shows the resolved absolute path.
Example files stay on the classpath under `dess-app/src/main/resources/examples/` and are copied
to that path only when the module is not initialized. Every write is validated, saved
atomically and preceded by a `.bak` copy of the previous file.

The frontend is a JSP on purpose: the architecture guard forbids servlet and JPA imports in
`src/main/java` for the institution module. `index.jsp` calls the CDI producers of the module
and never uses the XML infrastructure directly. `export.jsp` streams the saved configuration as
an attachment and uses the tree code as the file name.

## Design system

The UI/UX baseline is in [`docs/design/`](docs/design/). Read all five files before any frontend
work.

| File | What it covers |
| --- | --- |
| [UI_UX_DESIGN_SYSTEM.md](docs/design/UI_UX_DESIGN_SYSTEM.md) | Product experience, visual direction and design principles. |
| [DESIGN_TOKENS.md](docs/design/DESIGN_TOKENS.md) | Color, typography, spacing, radius, shadow and layout tokens. |
| [COMPONENT_CATALOG.md](docs/design/COMPONENT_CATALOG.md) | Shared components and their states. |
| [PAGE_SPECIFICATIONS.md](docs/design/PAGE_SPECIFICATIONS.md) | Page layouts and required content. |
| [UX_RULES.md](docs/design/UX_RULES.md) | UX rules and the acceptance checklist. |

The baseline targets Bootstrap 5, custom CSS and vanilla JavaScript. The application pages under
`WEB-INF/views/` and the institution structure page are implemented with it.

Two OpenCode subagents support the work. They are local only: the files are ignored by Git and
are not part of the repository.

- `.opencode/agents/frontend-developer.md` implements pages with the design system.
- `.opencode/agents/ui-reviewer.md` reviews JSP, CSS and JavaScript changes against it.

Use the named tokens and shared components. Do not invent a palette, type scale, spacing scale,
radius or shadow convention. Frontend changes must keep keyboard access, visible focus, contrast,
responsive behavior and loading, empty, error and success states. Do not claim that visual
behavior was verified unless the application was run and the page was inspected.

## Database migrations

Run Flyway at any time:

```bash
./ops/db/migrate.sh            # show status, then apply pending migrations
./ops/db/migrate.sh info       # status only
./ops/db/migrate.sh validate   # check applied migrations
./ops/db/migrate.sh repair     # repair the schema history after a failed migration
```

Extra arguments are passed to Maven:

```bash
./ops/db/migrate.sh migrate -Dflyway.target=2
```

Manage the optional PostgreSQL container directly:

```bash
docker compose --env-file .env -f ops/postgres/compose.yaml up -d
docker compose --env-file .env -f ops/postgres/compose.yaml stop
```

The container stores data in the `dess-postgres-data` volume. `stop` keeps it; `down` also
keeps the named volume unless you remove it explicitly.

Both `ops/db/migrate.sh` and `ops/wildfly/run.sh` check that a local `DESS_DB_PORT` accepts
connections before Maven or WildFly starts. If the container is healthy but its published port
is unreachable, the check prints the repair command, usually:

```bash
docker network connect postgres_default dess-postgres
```

Set `DESS_SKIP_DB_PORT_CHECK=1` to skip the check when the port is reachable from Java but not
from Bash.

## Persistence

`dess-app/src/main/resources/META-INF/persistence.xml` declares the JTA persistence unit
`dessPU`:

- datasource `java:/jdbc/DessDS`;
- Hibernate ORM supplied by WildFly;
- schema generation disabled with
  `jakarta.persistence.schema-generation.database.action=none` and
  `hibernate.hbm2ddl.auto=none`.

Entities exist for the people, academic and scheduling modules under their `infrastructure`
packages. Security accounts are still held in memory. Request account persistence before adding
an entity or migration for them. Flyway is the only component that changes the database schema.

## Branches and contributions

| Branch | Purpose |
| --- | --- |
| `main` | Release branch. It must always build. Only Sergio can push or merge (enforced by GitHub). |
| `dev` | Integration branch. Direct pushes are allowed. Only Sergio merges pull requests. |
| `feature/<area>-<topic>` | Short branch for one fix or feature. Open a pull request to `dev`. |

Rules:

- Do not push directly to `main`.
- Only Sergio merges pull requests into `main` and `dev`.
- Run `mvn -B -ntp test` before you push.
- Keep `main` and `dev` buildable.
- One review per merge.
- Put migrations in the same change as the feature that needs them.
- Keep secrets out of Git.

GitHub protection is enabled on `main`: only Sergio can push or merge, a pull request and one
review are required, the CI `build` check must pass, and force pushes and deletions are blocked.
`dev` blocks force pushes and deletions but allows direct pushes. Only Sergio merges pull
requests into `dev`; that part is a team rule. See [COLLABORATORS.md](COLLABORATORS.md) for the
full workflow.

## Documentation and comment style

- Use simple English and short sentences.
- Explain an acronym the first time you use it.
- Comments should explain why, not repeat the code.
- Keep docs and comments up to date with the code.
- Use English for code, comments, docs and commit messages.

## Boundaries

- JSP is presentation only. New pages use JSTL and Expression Language under `WEB-INF/views/`.
- Frontend changes follow `docs/design/`. Do not add a framework or build tool without an
  explicit architecture decision.
- Modules call each other through their `application` layer, not through repositories.
- `academic` owns teaching sessions; `assessment` owns exam sessions; `scheduling` owns rooms,
  bookings and conflict checks. Assessment and academic call scheduling to reserve resources.
- `shared` stays small and technical.
- One local WildFly instance and one local PostgreSQL database for this development setup.
- One institution per database. No tenant routing, cloud deployment or microservices in the MVP.
- For production, do not store datasource passwords as plain text in scripts. Use a WildFly
  credential store or deployment secret management.
