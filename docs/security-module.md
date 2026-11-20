# Security module

Status: **skeleton**. The packages and six main types exist. There is no behavior yet: no login,
no password hashing, no storage and no web filter.

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

## What each package does

| Package | Job |
| --- | --- |
| `security.domain` | Holds accounts, roles and permissions. It has no database or web code. |
| `security.application` | Defines the use cases: login, access checks and user management. |
| `security.infrastructure.persistence` | Will store accounts, roles and permissions. |
| `security.infrastructure.password` | Will hash and check passwords. |
| `security.infrastructure.session` | Will create and end login sessions. |
| `security.web.controller` | Will prepare data for the security pages. |
| `security.web.filter` | Will block requests that are not allowed. |
| `security.web.config` | Will wire the web layer for WildFly. |

## What exists

| Type | Kind | Purpose |
| --- | --- | --- |
| `UserAccount` | record | Login name, display name, password hash, active flag and roles. |
| `Role` | record | A named group of permissions. |
| `Permission` | record | One allowed action, identified by a code. |
| `AuthenticationService` | interface | Checks a username and password. |
| `AuthorizationService` | interface | Answers what an account may do. |
| `UserManagementService` | interface | Creates and manages accounts. |

The records have no validation or defensive copying yet. The interfaces have no
implementations. This is on purpose for the skeleton.

## Rules

- The domain does not depend on the database, CDI or the web layer.
- The application layer uses the domain and small interfaces. It does not use servlets or JPA.
- Infrastructure implements those interfaces.
- The web layer calls the application services. It does not contain business rules.
- Other modules use `security.application` only.

## What is missing

- Implementations of the three services.
- A storage choice and repository code.
- Password hashing and a password policy.
- Session handling.
- Login and user-management pages.
- A filter that protects the JSP pages.
- Tests for success and failure paths.

## Decisions needed before implementation

1. `ArchitectureTest` blocks `jakarta.persistence`, `jakarta.servlet` and `jakarta.ws.rs`
   imports in every file under `src/main/java`. It also blocks `@Entity`, `@Transactional` and
   similar annotations. Security storage and filters must use another approach, or the rule
   must be changed on purpose.
2. `dess-app/pom.xml` declares only the CDI API. Servlet and persistence APIs are not
   dependencies of the WAR yet.
3. The JSP page has no authentication or CSRF protection and is meant for localhost. A security
   filter must respect that, or the local-development rule must change.
4. The team plan gives `security` a dependency on `people`. That dependency is not in the code
   yet.

## Next step

Pick the storage and filter approach first. Then implement one use case end to end, starting
with `AuthenticationService`, and add tests.
