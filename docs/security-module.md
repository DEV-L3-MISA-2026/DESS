# Security module

Status: **working for local development**. Accounts, login, roles, CSRF checks and request
filtering are implemented. Accounts are held in memory; database persistence still needs an
architecture decision.

## Layout

```text
com.dess.security/
├── domain/
│   ├── UserAccount.java            # account record
│   ├── UserAccountRepository.java  # storage port
│   ├── Role.java
│   ├── Permission.java
│   └── RoleCatalog.java            # ADMIN and PROFESSOR definitions
├── application/
│   ├── AuthenticationService.java
│   ├── AuthorizationService.java
│   ├── UserManagementService.java
│   ├── spi/PasswordEncoder.java
│   └── impl/
│       ├── AuthenticationServiceImpl.java
│       ├── AuthorizationServiceImpl.java
│       ├── LoginThrottle.java
│       └── UserManagementServiceImpl.java
├── infrastructure/
│   ├── password/Pbkdf2PasswordEncoder.java
│   ├── persistence/InMemoryUserAccountRepository.java
│   └── session/
└── web/
    ├── SessionUser.java            # what the session stores
    ├── CsrfTokens.java
    ├── filter/
    │   ├── CsrfFilter.java
    │   └── SecurityFilter.java
    ├── servlet/
    │   ├── LoginServlet.java
    │   └── LogoutServlet.java
    ├── controller/AuthResource.java
    └── config/
```

## What each package does

| Package | Job |
| --- | --- |
| `security.domain` | Holds accounts, roles and permissions. It has no database or web code. |
| `security.application` | Defines login, access checks and account management. |
| `security.infrastructure` | Implements password hashing and account storage. |
| `security.web` | Servlets, filters, CSRF tokens and the session user type. |
| `security.web.controller` | JSON authentication endpoint. |

## What works

| Area | Behaviour |
| --- | --- |
| Passwords | PBKDF2-HMAC-SHA256, 210 000 iterations, 16-byte random salt per account, 256-bit hash, constant-time comparison. Encoded as `pbkdf2-sha256$iterations$salt$hash`. |
| Accounts | Stored in memory. `admin` and `professor` are seeded only when `DESS_ADMIN_PASSWORD` and `DESS_PROFESSOR_PASSWORD` are set. No password ships in the code. `ops/dev/seed-accounts.sh` writes local test values to `.env` for both roles. |
| Login | `POST /login` with a CSRF token. Success sends the browser to `next` when that path is local, otherwise to `/app`. |
| Session | Stores `SessionUser` (id, username, display name, roles). The session id changes after sign-in. The cookie is HTTP-only and the session expires after 30 minutes. |
| Logout | `POST /logout` with a CSRF token; it invalidates the session. |
| CSRF | One random 32-byte token per session, sent as the `csrfToken` form field or the `X-CSRF-Token` header. Any POST without a valid token gets `403` before authentication runs. |
| Throttling | Five failed logins in five minutes lock the username and client address for five minutes. State is in memory. |
| Authorization | `SecurityFilter` requires a session for every page except the public list. `/app/security` also requires the `ADMIN` role. Non-GET requests outside `/app/`, `/api/`, `/login`, `/logout` and `/assets/` require `institution:write`, so the institution editor cannot be changed through an alias path. API requests get a JSON `401` instead of a redirect. |
| Roles | `ADMIN` has the `ALL` permission. `PROFESSOR` may read institution, people, academic and scheduling data, and cannot change the institution structure. |
| Security page | Administrators can list accounts and create one with a username, display name, role and initial password. |

Public paths: `/login`, `/api/auth/login`, `/api/health`, `/assets/` and `/favicon.ico`.
Everything else requires a session. Error pages are rendered by internal forwards, not by public
URLs.

## Rules

- The domain does not depend on the database, CDI or the web layer.
- The application layer uses the domain and small interfaces. It does not use servlets or JPA.
- Infrastructure implements those interfaces.
- The web layer calls the application services. It does not contain business rules.
- Other modules use `security.application` only.

## What is missing

- Account persistence. Accounts are lost on restart, and the security page only creates them.
- A Flyway migration and a JPA entity for `app_user`. The `app_user` table exists but nothing
  maps it yet. Adding the mapping and migration needs Sergio's approval.
- Password policy, self-service password change and password reset.
- Editing and deactivating accounts from the page. The service supports it; the page does not.
- Audit logging for sign-in, sign-out and account changes.
- Concurrent-session control beyond sign-out.
- Tests against the real container. Current tests use in-memory fakes.

## Decisions needed before further work

1. **Account storage.** Choose between a JPA entity mapped to `app_user` and reading accounts
   through a dedicated store. JPA would add a migration and entity under
   `security.infrastructure.persistence`. `ArchitectureTest` allows JPA only in the
   infrastructure, cdi and application/impl packages of the modules it knows; security would
   have to be added to those allowances on purpose.
2. **Role model.** The plan gives `security` a dependency on `people`. The code does not use it
   yet. Decide whether accounts link to person records.
3. **Production use.** The application is local-only: no TLS, no credential store and no
   hardening for a public network.

## Next step

Decide the account storage approach. Then persist accounts, keep the environment seeding for
the first start, and extend the security page to edit, deactivate and delete accounts.
