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
| Backend Framework | Spring Boot 3.5.x (Spring MVC, Spring Security, Spring Data JPA) |
| Frontend Apps | Next.js 15 / React 19 / Tailwind CSS 4 (`storefront/` and `dashboard/`) |
| Database | PostgreSQL 16 (schema owned by Flyway migrations `V2`–`V18`) |
| Caching & Events | Redis (with automatic in-memory `ConcurrentMapCacheManager` fallback) + Spring async events |
| Auth | JWT (access + refresh), BCrypt, RBAC, rate limiting, server-side token revocation |
| Docs | Swagger/OpenAPI (springdoc) |
| Payments | Full & installment payment engine + HMAC-SHA256 webhooks (Korapay integration ready) |
| Build | Maven + Maven Wrapper (no global install needed) |
| Hosting | Render (`render.yaml` Blueprint for API + Postgres) + Vercel (`vercel.json` Services for frontends) |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |
| Tests | JUnit 5, MockMvc, H2 (Testcontainers when we hit Postgres-only SQL) |
| Coverage | JaCoCo — line ≥ 80%, branch ≥ 55% enforced in `verify`; history & badge via Codecov |

## Quickstart

Prerequisites:

- **JDK 21** (Temurin recommended — via [SDKMAN](https://sdkman.io): `sdk install java 21.0.5-tem`)
- **PostgreSQL** (any recent version). No Maven needed — the wrapper handles it.

```bash
# 1. Start Postgres (Docker) — or use your own local instance
docker run -d --name meethybridhub-db \
  -e POSTGRES_DB=meethybridhub -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 postgres:16

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
./scripts/db.sh start    # initdb on first run, then start on :5433
./scripts/db.sh status
./scripts/db.sh psql     # interactive psql (user: postgres / password: postgres)
./scripts/db.sh stop
```

The cluster lives in `~/pgdata`, the server log in `~/pgdata/server.log`, and the
app connects with the `application.yml` defaults (`jdbc:postgresql://localhost:5433/meethybridhub`,
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
├── config/                         # Security, OpenAPI, JPA auditing, scheduling,
│                                   # Redis + fallback cache, async events, secrets validator
├── common/api/                     # ApiError, GlobalExceptionHandler
├── common/exception/               # domain exceptions (400, 401, 403, 404, 429)
├── api/ping/                       # smoke-test endpoint
├── identity/                       # users, JWT auth, email verification,
│                                   # password reset, RBAC, rate limiting,
│                                   # security audit trail (audit_log),
│                                   # server-side logout (revoked_tokens)
├── store/                          # multi-tenancy: stores, domains,
│                                   # StoreFilter, TenantContext, TenantEntity,
│                                   # admin store management, branding/settings
├── catalog/                        # categories, products, variants, inventory,
│                                   # public store catalog endpoints
├── orders/                         # orders, order items, hybrid guest/account checkout
├── payments/                       # payments, HMAC webhooks, installment plans & schedules
├── customorders/                   # bespoke requests, quotes, messages, attachments,
│                                   # quote-to-order conversion
├── discovery/                      # store & product reviews, favorites, featured content,
│                                   # store search
├── admin/                          # platform & store analytics, commissions, disputes,
│                                   # platform configuration, sales snapshots
└── billing/                        # dormant flat-fee platform charge module
src/main/resources/
├── application.yml                 # main config (env-var driven)
├── application-test.yml            # H2 test profile
└── db/migration/                   # Flyway migrations (V2–V18)
storefront/                         # Next.js 15 multi-store customer storefront (/)
dashboard/                          # Next.js 15 store-owner & admin dashboard (/dashboard)
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
   1. `X-Store-Id` header — explicit numeric tenant ID for API clients
   2. `X-Store-Slug` header — explicit store slug (sent automatically by `storefront/` and `dashboard/` based on the active hostname)
   3. **Subdomain** from the `Host` header, e.g.
      `divinez-signature.meethybridhub.com` → slug `divinez-signature`
   4. `storeId` JWT claim — the store the token was issued for

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
| GET | `/stores/me/analytics` | Store analytics & sales metrics | Owner/`ADMIN` |
| GET | `/public/stores/{slug}` | Public store profile by slug | Public |
| GET | `/public/stores/{slug}/categories` | Public store categories | Public |
| GET | `/public/stores/{slug}/products` | Public store products (paginated/filtered) | Public |
| GET | `/public/stores/{slug}/products/{productId}` | Public product detail | Public |
| POST / GET | `/categories` | Create / list store categories | Owner/`ADMIN` / Public |
| PUT / DELETE | `/categories/{categoryId}` | Update / delete category | Owner/`ADMIN` |
| POST / GET | `/products` | Create / list store products | Owner/`ADMIN` / Public |
| GET | `/products/search` | Search products in active store | Public |
| GET / PUT / DELETE | `/products/{productId}` | Get / update / delete product | Public / Owner/`ADMIN` |
| POST / GET | `/products/{productId}/variants` | Create / list product variants | Owner/`ADMIN` / Public |
| GET / PUT | `/variants/{variantId}/inventory` | Get / update variant stock | Owner/`ADMIN` |
| POST / GET | `/orders` | Create order (guest or authenticated) / list orders | Public / Authenticated |
| GET | `/orders/{orderId}` | Order detail | Authenticated |
| PUT | `/orders/{orderId}/cancel` | Cancel order | Authenticated |
| PUT | `/orders/{orderId}/status` | Update order status | Owner/`ADMIN` |
| POST | `/orders/{orderId}/payments` | Initiate payment for an order | Public / Authenticated |
| GET | `/payments/{paymentId}` | Get payment status | Authenticated |
| POST | `/payments/webhook` | Payment provider webhook (HMAC-SHA256) | Public (signed) |
| POST | `/orders/{orderId}/installments` | Create installment plan (account required) | Authenticated |
| GET | `/installments/{planId}` | Get installment plan & schedule | Authenticated |
| POST / GET | `/custom-orders` | Submit / list custom order requests | Authenticated |
| GET / PUT | `/custom-orders/{id}`, `/custom-orders/{id}/status` | Custom order detail / update status | Authenticated / Owner |
| POST / GET | `/custom-orders/{requestId}/quotes` | Create / list quotes for custom request | Owner / Authenticated |
| POST | `/custom-orders/quotes/{quoteId}/accept` (or `/reject`) | Accept or reject quote | Authenticated |
| POST / GET | `/custom-orders/{requestId}/messages` | Custom order conversation messages | Authenticated |
| POST / GET | `/custom-orders/{requestId}/attachments` | Custom order attachments | Authenticated |
| POST | `/custom-orders/{requestId}/convert` | Convert accepted quote into an order | Authenticated |
| POST / GET | `/discovery/stores/{storeId}/reviews` | Submit / list store reviews (+ `/summary`) | Authenticated / Public |
| POST / GET | `/discovery/products/{productId}/reviews` | Submit / list product reviews | Authenticated / Public |
| POST / GET / DELETE | `/discovery/favorites` | Manage saved favorite stores/products | Authenticated |
| GET | `/discovery/featured` (`/stores`, `/products`, `/banners`) | Featured discovery content | Public |
| GET | `/discovery/search/stores` | Search active stores | Public |
| GET / PUT | `/admin/config`, `/admin/config/{key}` | Platform configuration | `ADMIN` |
| POST / GET / PUT | `/admin/commissions/rules`, `/entries`, `/summary` | Commission rules & ledger | `ADMIN` |
| POST / GET / PUT | `/admin/disputes`, `/admin/disputes/{id}/status`, `/messages` | Dispute resolution & messaging | Authenticated / `ADMIN` |
| GET | `/admin/analytics/platform`, `/admin/analytics/stores/{storeId}` | Platform & per-store analytics | `ADMIN` |

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
| `V8__platform_charges.sql` | `platform_charges` — dormant flat-fee platform charge ledger |
| `V9__store_settings.sql` | `store_settings` — branding (logo, colors, theme, tagline) |
| `V10__revoked_tokens.sql` | `revoked_tokens` — server-side logout denylist |
| `V11__catalog.sql` | `categories`, `products` |
| `V12__product_variants_inventory.sql` | `product_variants`, `inventory` |
| `V13__orders.sql` | `orders`, `order_items` (hybrid guest + account checkout) |
| `V14__payments.sql` | `payments` (idempotency keys, webhook verification) |
| `V15__installments.sql` | `installment_plans`, `installment_payments` |
| `V16__custom_orders.sql` | `custom_order_requests`, `quotes`, `custom_order_conversations`, `custom_order_attachments` |
| `V17__discovery.sql` | `store_reviews`, `product_reviews`, `user_favorites`, `featured_content`, `recommendation_scores` |
| `V18__admin_analytics.sql` | `platform_config`, `commission_rules`, `commission_entries`, `disputes`, `dispute_messages`, `sales_snapshots` |

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
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `localhost:5433/meethybridhub`, `postgres`/`postgres` | PostgreSQL connection |
| `PORT` | `8080` | Server port |
| `JWT_SECRET` | dev placeholder (⚠️ change in prod) | HMAC secret, ≥ 32 bytes |
| `JWT_ACCESS_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | `24` h / `30` d | Token lifetimes |
| `STORE_BASE_DOMAIN` | `meethybridhub.com` | Subdomain tenant resolution |
| `APP_BASE_URL` | `http://localhost:8080` | Verification-link base URL |
| `AUTH_MAX_FAILED_ATTEMPTS` / `AUTH_MAX_ATTEMPTS_PER_IP` | `5` / `20` | Login lockout window |
| `AUTH_MAX_EMAILS_PER_EMAIL` / `AUTH_MAX_EMAILS_PER_IP` | `3` / `10` | Email-flood limits |
| `MAIL_*` | Gmail SMTP defaults | Email sending |

## Deploying

Production runs the API and its database on **Render** and the two Next.js apps
on **Vercel**. Full runbook (order of operations, env vars, bootstrap SQL,
verification and troubleshooting): **[docs/deploy-render-vercel.md](docs/deploy-render-vercel.md)**.

| Piece | Host | Config |
|---|---|---|
| API (Docker) + PostgreSQL 16 | Render | `render.yaml` |
| Storefront + dashboard | Vercel | `vercel.json` |

Deploy Vercel first (its origin goes into the API's `CORS_ALLOWED_ORIGINS`), then
`render.yaml` as a Render Blueprint, then set `NEXT_PUBLIC_API_URL` in Vercel to
the Render URL and redeploy — that value is inlined at build time. The API reads
`DB_URL` when set, otherwise composes the JDBC URL from `DB_HOST` / `DB_PORT` /
`DB_NAME` (Render exposes those separately, and its `postgres://` string is not a
JDBC URL).

### Deploying the frontends (Vercel)

`vercel.json` deploys the two Next.js apps as one Vercel project with two services:

| Service | Directory | Public path |
|---|---|---|
| `storefront` | `storefront/` | `/` (everything not under `/dashboard`) |
| `dashboard` | `dashboard/` | `/dashboard` (Next.js `basePath`) |

Neither app calls the other, so there are no service bindings. Both call this
Spring Boot API from the browser. The API itself (with PostgreSQL and Redis)
does not run on Vercel; host it separately, then set:

- In Vercel, for both services: `NEXT_PUBLIC_API_URL` = the API's public URL
  (it is inlined at build time, so redeploy after changing it). Both `storefront`
  and `dashboard` automatically detect the active store (`meethybridhub` vs
  `divinez-signature`) from the request hostname, falling back to
  `NEXT_PUBLIC_STORE_SLUG`.
- To give each store owner their own URL on a single Vercel project, assign two
  domains under **Vercel → Project Settings → Domains** (e.g.
  `meethybridhub.vercel.app` and `divinez-signature.vercel.app`).
- On the API: `CORS_ALLOWED_ORIGINS` = comma-separated Vercel domains (e.g.
  `https://meethybridhub.vercel.app,https://divinez-signature.vercel.app`).

Run `vercel dev` from the repository root to serve both services together
locally. Running the dashboard on its own (`npm run dev` in `dashboard/`)
serves it under `/dashboard` as well.

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` and every
pull request. The badge above reflects the latest run on `main`.

| Step | What it does |
|---|---|
| Checkout + JDK 21 (Temurin) | Uses the committed Maven Wrapper (`./mvnw`) — same Maven version everywhere |
| `./mvnw -B verify` | Compiles, runs all tests (H2-backed, no DB needed), packages the jar, and enforces the JaCoCo coverage floor |
| Upload test reports | Archives `target/surefire-reports/` as a downloadable artifact (7 days) |
| Publish test results | `dorny/test-reporter` posts a **Maven Tests** check run with per-line annotations on the PR — also on failure |
| Upload coverage report | Archives the JaCoCo HTML/XML report (`target/site/jacoco/`) as an artifact (7 days) |
| Upload to Codecov | `codecov-action` uploads `jacoco.xml` → coverage history, README badge, PR comments (status checks in `codecov.yml`) |

- **One run per ref** — a new push cancels the stale in-progress run.
- These are the same tests you run locally: `./mvnw test` (H2 in-memory).
- **Coverage floor**: `./mvnw verify` fails if line coverage drops below 80% or branch below
  55% (thresholds in `pom.xml`). View the full report locally by opening
  `target/site/jacoco/index.html` after a `verify`.
- **Codecov** ([codecov.io/gh/Meet-hybrid/meethybridhub](https://codecov.io/gh/Meet-hybrid/meethybridhub))
  tracks coverage over time and posts a comment on PRs. Tokenless upload works for public
  repos; if the repo is private, add a `CODECOV_TOKEN` repo secret first.
- Enable **branch protection** on `main` to make a green CI a merge requirement.

## Roadmap

1. Foundations — Spring Boot 3.5, Java 21, Flyway, OpenAPI, global error handling, CI/CD ✅
2. Identity — JWT auth, users, roles, email verification, rate limiting, server-side logout, audit log ✅
3. Tenancy + stores — store registration, `X-Store-Id` / `X-Store-Slug` / subdomain resolution, admin management, branding/settings ✅
4. Catalog — categories, products, variants, inventory, public store catalog API ✅
5. Orders + payments — hybrid guest/account checkout, payments, HMAC webhooks, idempotency, **installment plans** ✅ *(live Korapay credentials pending)*
6. Custom orders — request → quote → conversation/attachments → order conversion workflow ✅
7. Discovery — store & product reviews, favorites, featured stores/products/banners, store search ✅
8. Admin + analytics — user & store management, store & platform analytics, commissions, disputes, platform config ✅
9. Hardening — Redis caching with automatic in-memory fallback, async domain events, production startup secrets validator ✅
10. Deploy & Frontends — Multi-store Next.js 15 `storefront/` & `dashboard/`, Docker, Render Blueprint (`render.yaml`), Vercel Services (`vercel.json`) ✅
