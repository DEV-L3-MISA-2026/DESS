# Security module — package structure

Status: **scaffold only**. The packages below exist together with the three domain records and
the three application service interfaces. They declare data and method signatures only; no
behavior is implemented and no infrastructure or web adapters exist yet. Each directory also
contains a `package-info.java` file that records its planned responsibility.

## Layout

```text
com.dess.security/
├── domain/
│   ├── UserAccount.java
│   ├── Role.java
│   └── Permission.java
├── application/
│   ├── AuthenticationService.java
│   ├── AuthorizationService.java
│   └── UserManagementService.java
├── infrastructure/
│   ├── persistence/
│   ├── password/
│   └── session/
└── web/
    ├── controller/
    ├── filter/
    └── config/
```

## Scaffolded types

| Type | Kind | Purpose |
| --- | --- | --- |
| `UserAccount` | record | Account identity, login name, encoded password, activation state and roles. |
| `Role` | record | Named group of permissions. |
| `Permission` | record | Unit of authorization identified by a code. |
| `AuthenticationService` | interface | Verifies credentials and returns the authenticated account. |
| `AuthorizationService` | interface | Answers permission and role questions for an account. |
| `UserManagementService` | interface | Creates, updates, deactivates and deletes accounts and role assignments. |

The records currently expose their components without validation, normalization or defensive
copying, and the interfaces have no implementations. Both are intentional for this scaffold.

## Responsibilities

| Package | Responsibility |
| --- | --- |
| `security` | Module root; defines the public surface and dependency rules. |
| `security.domain` | User account aggregate plus role and permission value objects. Framework-independent. |
| `security.application` | Authentication, authorization and user-management use cases. Orchestrates the domain and calls infrastructure through ports. |
| `security.infrastructure.persistence` | Repository adapters for accounts, roles and permissions. |
| `security.infrastructure.password` | Password hashing and verification. Plain-text passwords never leave this package. |
| `security.infrastructure.session` | Authenticated session or token lifecycle. Stores no business data. |
| `security.web` | Delivery layer for the JSP frontend. |
| `security.web.controller` | Translates HTTP input into application calls and prepares view data. |
| `security.web.filter` | Establishes the current user and rejects unauthenticated or unauthorized requests. |
| `security.web.config` | Wires filters, controllers and view resources for WildFly. |

## Dependency rules

- `domain` depends on the JDK only.
- `application` depends on `domain` and on port interfaces; it does not depend on
  `infrastructure` or `web`.
- `infrastructure` implements the ports and may depend on `application` and `domain`.
- `web` depends on `application` (and on the session adapter); it contains no business rules.
- Other modules call `security.application` only and never reach into `domain` or
  `infrastructure`.

## Constraints to resolve before implementation

1. `ArchitectureTest` currently rejects `jakarta.persistence`, `jakarta.servlet` and
   `jakarta.ws.rs` imports, plus `@Entity`, `@Transactional` and similar annotations, in every
   source under `src/main/java`. Implementing JPA repositories in
   `infrastructure.persistence` or servlet filters in `web.filter` therefore requires either an
   explicit exception for the security module or a different adapter technology.
2. `dess-app/pom.xml` declares only the CDI API. Servlet and persistence APIs are managed by the
   parent POM but are not yet dependencies of the WAR.
3. The frontend is intentionally JSP-only and has no authentication or CSRF protection. Any
   security filter must stay compatible with that local-development constraint or the
   constraint must be revisited explicitly.
