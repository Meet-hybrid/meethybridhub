# MeethybridHub API

Production-ready **multi-tenant e-commerce SaaS** — fashion-first, catalog-agnostic.
Every business gets its own branded storefront (subdomain + future custom domains),
all stores share one customer account, and **installment payments** are a first-class
feature.

[![CI](https://github.com/Meet-hybrid/meethybridhub/actions/workflows/ci.yml/badge.svg)](https://github.com/Meet-hybrid/meethybridhub/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Meet-hybrid/meethybridhub/graph/badge.svg)](https://codecov.io/gh/Meet-hybrid/meethybridhub)

## Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x (Spring MVC, Spring Security, Spring Data JPA) |
| Database | PostgreSQL (schema owned by Flyway migrations) |
| Auth | JWT (access + refresh), BCrypt, RBAC, rate limiting |
| Docs | Swagger/OpenAPI (springdoc) |
| Payments | Korapay (Phase 5) |
| Media | Cloudinary (Phase 6) |
| Build | Maven + Maven Wrapper (no global install needed) |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |
| Tests | JUnit 5, MockMvc, H2 (Testcontainers when we hit Postgres-only SQL) |
| Coverage | JaCoCo — line ≥ 80%, branch ≥ 55% enforced in `verify`; history & badge via Codecov (as of Aug 2026: 91% / 74%) |

## Quickstart

Prerequisites:

- **JDK 21** (Temurin recommended — via [SDKMAN](https://sdkman.io): `sdk install java 21.0.5-tem`)
- **PostgreSQL** (any recent version). No Maven needed — the wrapper handles it.

```bash
# 1. Start Postgres (Docker) — or use your own local instance
docker run -d --name meethybridhub-db \
  -e POSTGRES_DB=meethybridhub -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16

# 2. Run the API (first run downloads the pinned Maven version;
#    Flyway applies the db/migration scripts on startup)
./mvnw spring-boot:run

# 3. Open the docs
#    Swagger UI: http://localhost:8080/swagger-ui.html
#    Health:     http://localhost:8080/actuator/health
```

Run tests without any database (tests use in-memory H2):

```bash
./mvnw test
```

> **Maven Wrapper:** `mvnw` pins Maven 3.9.11 (see
> `.mvn/wrapper/maven-wrapper.properties`). The first run downloads Maven and the
> wrapper jar into `~/.m2/wrapper/` — no global Maven install required.

### Local PostgreSQL without root (this machine)

This dev machine has no `sudo`, so PostgreSQL 16 was installed rootlessly by
**extracting the Ubuntu noble .debs** (`postgresql-16`, `postgresql-client-16`,
`libpq5`) into `~/pgroot` — the package scripts were intentionally bypassed.
A helper script manages the single-user cluster:

```bash
./scripts/db.sh start    # initdb on first run, then start on :5432
./scripts/db.sh status
./scripts/db.sh psql     # interactive psql (user: postgres / password: postgres)
./scripts/db.sh stop
```

The cluster lives in `~/pgdata`, the server log in `~/pgdata/server.log`, and the
app connects with the `application.yml` defaults (`jdbc:postgresql://localhost:5432/meethybridhub`,
user `postgres` / password `postgres`). The `meethybridhub` database is created
automatically on first `start`. Dev-only credentials; production secrets come
from env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

> Why the `.deb` extraction? On this network, Docker Hub is unreachable (broken
> IPv6) and apt needs root. Extracting `.debs` with `dpkg-deb -x` gives us the
> real binaries without root — the trade-off is that package postinst scripts
> (which register systemd services and system users) are skipped, hence the
> helper script.

## Project structure

```
src/main/java/com/meethybridhub/
├── MeethybridHubApplication.java   # entry point
├── config/                         # Security, OpenAPI, JPA auditing, scheduling
├── common/api/                     # ApiError, GlobalExceptionHandler
├── common/exception/               # domain exceptions (400, 401, 403, 404, 429)
├── api/ping/                       # smoke-test endpoint
├── identity/                       # users, JWT auth, email verification,
│                                   # password reset, RBAC, rate limiting,
│                                   # security audit trail (audit_log),
│                                   # server-side logout (revoked_tokens)
└── store/                          # multi-tenancy: stores, domains,
                                    # StoreFilter, TenantContext, TenantEntity,
                                    # admin store management, branding/settings
src/main/resources/
├── application.yml                 # main config (env-var driven)
├── application-test.yml            # H2 test profile
└── db/migration/                   # Flyway migrations (V2–V10)
src/test/java/com/meethybridhub/
└── (integration + unit tests, H2-backed)
```

## Architecture

**Layered monolith, packaged by feature.** One Spring Boot jar + one Postgres
database. Within each feature package the layers are classic:
Controller → Service → Repository → Entity.

### Request lifecycle

Every request passes through two custom servlet filters (wired in
`config/SecurityConfig`):

1. **`JwtAuthenticationFilter`** — extracts the Bearer token, validates
   signature, expiry, and the **password version** claim (tokens issued before a
   password change are rejected), then loads the user and populates Spring
   Security's `SecurityContext`. Stateless: no server-side session, so any
   instance can serve any request.
2. **`StoreFilter`** — resolves the tenant (store) for the request and stores it
   in `TenantContext` (a `ThreadLocal`, safe under virtual threads), clearing it
   afterwards. Resolution order (first match wins):
   1. `X-Store-Id` header — explicit tenant for API clients
   2. **Subdomain** from the `Host` header, e.g.
      `divine-signature.meethybridhub.com` → slug `divine-signature`
   3. `storeId` JWT claim — the store the token was issued for

> **Security note:** resolution is not authorization. `X-Store-Id` is
> client-controlled; the ownership check in `StoreService.getCurrentTenantStore`
> is what prevents a store owner from reaching another store's data (admins
> bypass it). Every store-scoped operation must funnel through that method.

### Multi-tenancy — shared schema, row-level `store_id`

- One database, shared tables; every tenant-scoped entity extends
  `TenantEntity` (gains a non-null `store_id` column).
- Repositories extend `TenantRepository`, which ships store-filtered read methods
  (`findAllByStoreId`, `findByIdAndStoreId`, `existsByIdAndStoreId`,
  `countByStoreId`) — the single enforcement point for row-level isolation.
- `StoreFilter` + `TenantContext` + `TenantRepository` together keep each store's
  data isolated (same pattern as Shopify).

### Auth & identity

- JWT **access** (24h) + **refresh** (30d) tokens; `storeId` and password-version
  claims.
- **Server-side logout** — `POST /auth/logout` revokes the refresh token via a
  `revoked_tokens` denylist (only SHA-256 hashes stored); `/refresh` rejects
  revoked tokens. Access tokens expire naturally (24h), keeping the stateless
  design free of per-request DB lookups.
- **BCrypt** password hashing; roles stored on the user (`CUSTOMER`,
  `STORE_OWNER`, `ADMIN`) with `@PreAuthorize` method security.
- Email verification with expiring tokens; password reset with short-lived tokens.
- Defense in depth: login rate limiting + account lockout (per email/IP, windowed),
  email-flood protection on resend-verification/reset-password, and a scheduled
  `TokenCleanupService` purging expired tokens.
- **Security audit trail** — every security-relevant event (registration, login
  success/failure, email verification, password changes, admin actions, store
  lifecycle) is appended to the `audit_log` table via `AuditLogService`
  (best-effort writes that join the caller's transaction, keeping the `user_id`
  FK valid). `ClientIpResolver` captures the client IP for auth + admin events,
  honoring the `AUTH_TRUST_FORWARDED_HEADER` setting.

## API

Base path: `/api/v1` · Interactive docs at `/swagger-ui.html` (JWT bearer auth).

| Method | Path | Description | Access |
|---|---|---|---|
| GET | `/ping` | Smoke test | Public |
| POST | `/auth/register` | Create account (+ email verification) | Public |
| POST | `/auth/login` | Authenticate → access + refresh tokens | Public |
| POST | `/auth/refresh` | Rotate refresh token | Public |
| POST | `/auth/logout` | Revoke refresh token (server-side denylist) | Public |
| GET | `/auth/verify` | Verify email (token query param) | Public |
| POST | `/auth/resend-verification` | Resend verification email | Public |
| POST | `/auth/reset-password` | Request password reset (email) | Public |
| POST | `/auth/reset-password/confirm` | Set new password with token | Public |
| GET | `/users/me` | Current profile | Any authenticated |
| PUT | `/users/me` | Update profile | Any authenticated |
| POST | `/users/me/change-password` | Change password | Any authenticated |
| DELETE | `/users/me` | Delete account | Any authenticated |
| GET | `/admin/users` | List users | `ADMIN` |
| GET | `/admin/users/{id}` | User detail | `ADMIN` |
| PUT | `/admin/users/{id}/roles` | Change roles | `ADMIN` |
| PUT | `/admin/users/{id}/status` | Suspend/activate | `ADMIN` |
| DELETE | `/admin/users/{id}` | Delete user | `ADMIN` |
| GET | `/admin/stores` | List stores (filter by `?status=`) | `ADMIN` |
| PUT | `/admin/stores/{id}/status` | Suspend/activate a store | `ADMIN` |
| POST | `/stores` | Register a store (grants `STORE_OWNER`) | Authenticated |
| GET | `/stores/me` | Current tenant store | Owner/`ADMIN` |
| GET | `/stores/me/domains` | Store domains | Owner/`ADMIN` |
| POST | `/stores/me/domains` | Register a domain | Owner/`ADMIN` |
| GET | `/stores/me/settings` | Store branding/settings | Owner/`ADMIN` |
| PUT | `/stores/me/settings` | Update branding (logo, colors, theme) | Owner/`ADMIN` |

> **New endpoints are public by default** unless they match a URL rule in
> `SecurityConfig` or carry `@PreAuthorize` — add one of the two when introducing
> a controller.

## Database

The schema is owned entirely by **Flyway** migrations under
`src/main/resources/db/migration/`; Hibernate runs with `ddl-auto: validate` so
the app refuses to start if entities drift from the DB.

| Migration | Contents |
|---|---|
| `V2__identity.sql` | `users`, `email_verification_tokens`, `password_reset_tokens`, `login_attempts`, `audit_log` |
| `V3__stores.sql` | `stores`, `store_domains` (multi-tenancy) |
| `V4__add_password_version.sql` | Password-version column (token invalidation on password change) |
| `V5__login_attempts_ip_to_varchar.sql` | IP column type alignment |
| `V6__login_attempts_add_purpose.sql` | Purpose column (login vs. email-send counters) |
| `V7__audit_log_ip_to_varchar.sql` | IP column type alignment (`audit_log`) |
| `V9__store_settings.sql` | `store_settings` — branding (logo, colors, theme, tagline) |
| `V10__revoked_tokens.sql` | `revoked_tokens` — server-side logout denylist |

## Locked decisions (so far)

1. **Checkout model — hybrid.** Guest checkout for full payments; **account
   required for installment payments** (you're extending credit); one-click
   post-purchase account capture pre-seeded with the guest order.
   Consequence: `orders.customer_id` is nullable + contact fields for guests.
2. **Multi-tenancy — shared schema.** One Postgres DB, `store_id` on every
   tenant-scoped table, composite indexes leading with `store_id`.
3. **Store URLs — Host-header resolution.** One backend, wildcard DNS, a
   `store_domains` table (subdomain + verified custom domain). Same pattern
   as Shopify.
4. **Framework line — Spring Boot 3.5.x** (not 4.x): stable, documented, and
   fully compatible with Java 21. We upgrade majors deliberately, later.
5. **Stateless auth.** JWT only, no server-side sessions — horizontal scaling
   stays trivial (any instance can serve any request).

## Configuration

Every environment-specific value is an environment variable with a safe local
default (see `application.yml` and `.env.example`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `localhost:5432/meethybridhub`, `postgres`/`postgres` | PostgreSQL connection |
| `PORT` | `8080` | Server port |
| `JWT_SECRET` | dev placeholder (⚠️ change in prod) | HMAC secret, ≥ 32 bytes |
| `JWT_ACCESS_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | `24` h / `30` d | Token lifetimes |
| `STORE_BASE_DOMAIN` | `meethybridhub.com` | Subdomain tenant resolution |
| `APP_BASE_URL` | `http://localhost:8080` | Verification-link base URL |
| `AUTH_MAX_FAILED_ATTEMPTS` / `AUTH_MAX_ATTEMPTS_PER_IP` | `5` / `20` | Login lockout window |
| `AUTH_MAX_EMAILS_PER_EMAIL` / `AUTH_MAX_EMAILS_PER_IP` | `3` / `10` | Email-flood limits |
| `MAIL_*` | Gmail SMTP defaults | Email sending |

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` and every
pull request. The badge above reflects the latest run on `main`.

| Step | What it does |
|---|---|
| Checkout + JDK 21 (Temurin) | Uses the committed Maven Wrapper (`./mvnw`) — same Maven version everywhere |
| `./mvnw -B verify` | Compiles, runs all tests (H2-backed, no DB needed), packages the jar, and enforces the JaCoCo coverage floor |
| Upload test reports | Archives `target/surefire-reports/` as a downloadable artifact (7 days) |
| Publish test results | `dorny/test-reporter` posts a **Maven Tests** check run with per-line annotations on the PR — also on failure || Upload coverage report | Archives the JaCoCo HTML/XML report (`target/site/jacoco/`) as an artifact (7 days) |
| Upload to Codecov | `codecov-action` uploads `jacoco.xml` → coverage history, README badge, PR comments (status checks in `codecov.yml`) |

- **One run per ref** — a new push cancels the stale in-progress run.
- These are the same tests you run locally: `./mvnw test` (148 tests, H2 in-memory).
- **Coverage floor**: `./mvnw verify` fails if line coverage drops below 80% or branch below
  55% (thresholds in `pom.xml`; as of Aug 2026 the suite measures 91.2% line / 74.1% branch). View
  the full report locally by opening `target/site/jacoco/index.html` after a `verify`.
- **Codecov** ([codecov.io/gh/Meet-hybrid/meethybridhub](https://codecov.io/gh/Meet-hybrid/meethybridhub))
  tracks coverage over time and posts a comment on PRs. Tokenless upload works for public
  repos; if the repo is private, add a `CODECOV_TOKEN` repo secret first.
- Enable **branch protection** on `main` to make a green CI a merge requirement.

## Roadmap

1. Foundations ✅
2. Identity — JWT auth, users, roles, email verification, rate limiting ✅
3. Tenancy + stores — store registration, subdomain resolution, admin management, branding/settings ✅
4. Catalog — categories, products, inventory, reviews
5. Orders + payments — Korapay, webhooks, idempotency, **installments**
6. Custom orders — request → quote → order workflow
7. Discovery — featured/recommended stores
8. Admin + analytics — user & store management ✅; reports, commissions, disputes
9. Hardening — Redis caching, queues, observability
10. Deploy — Docker, Flyway migrations, backups
