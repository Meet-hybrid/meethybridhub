# MeethybridHub API

Production-ready **multi-tenant e-commerce SaaS** — fashion-first, catalog-agnostic.
Every business gets its own branded storefront (subdomain + future custom domains),
all stores share one customer account, and **installment payments** are a first-class
feature.

## Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x (Spring MVC, Spring Security, Spring Data JPA) |
| Database | PostgreSQL (schema owned by Flyway migrations) |
| Auth | JWT (Phase 2) |
| Docs | Swagger/OpenAPI (springdoc) |
| Payments | Korapay (Phase 5) |
| Media | Cloudinary (Phase 6) |
| Build | Maven + Maven Wrapper (no global install needed) |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |
| Tests | JUnit 5, MockMvc (H2 in tests; Testcontainers when we hit Postgres-only SQL) |

## Quickstart

Prerequisites:

- **JDK 21** (Temurin recommended — via [SDKMAN](https://sdkman.io): `sdk install java 21.0.5-tem`)
- **PostgreSQL** (any recent version). No Maven needed — the wrapper handles it.

```bash
# 1. Start Postgres (Docker) — or use your own local instance
docker run -d --name meethybridhub-db \
  -e POSTGRES_DB=meethybridhub -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16

# 2. Run the API (first run downloads the pinned Maven version)
./mvnw spring-boot:run

# 3. Open the docs
#    Swagger UI: http://localhost:8080/swagger-ui.html
#    Health:     http://localhost:8080/actuator/health
```

Run tests without any database (tests use in-memory H2):

```bash
./mvnw test
```

> Note: no Flyway migrations exist yet — the first one (`V1__init.sql`) is
> written during the database design phase.

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
├── config/                         # SecurityConfig, OpenApiConfig
├── common/api/                     # ApiError, GlobalExceptionHandler
├── common/exception/               # domain exceptions (404, 400, ...)
└── api/ping/                       # smoke-test endpoint (feature-first packages)
src/main/resources/
├── application.yml                 # main config (env-var driven)
├── application-test.yml            # H2 test profile
└── db/migration/                   # Flyway migrations (empty until DB phase)
```

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

## Roadmap

1. Foundations ✅ (this scaffold)
2. Identity — JWT auth, users, roles, email verification
3. Tenancy + stores — store registration, subdomain resolution, branding
4. Catalog — categories, products, inventory, reviews
5. Orders + payments — Korapay, webhooks, idempotency, **installments**
6. Custom orders — request → quote → order workflow
7. Discovery — featured/recommended stores
8. Admin + analytics — reports, commissions, disputes
9. Hardening — Redis caching, queues, observability
10. Deploy — Docker, Flyway migrations, backups
