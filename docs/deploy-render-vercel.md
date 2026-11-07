# Deploying MeethybridHub (Render + Vercel)

How this repo ships:

| Piece | Host | What it is | Config |
|---|---|---|---|
| API | **Render** (Docker web service) | Spring Boot 3.5, Java 21 | `render.yaml`, `Dockerfile` |
| Database | **Render Postgres 16** | Flyway owns the schema | `render.yaml` (`databases`) |
| Storefront | **Vercel** (service `/`) | Next.js 15 app in `storefront/` | `vercel.json` |
| Dashboard | **Vercel** (service `/dashboard`) | Next.js 15 app in `dashboard/` | `vercel.json` |

`vercel.json` uses Vercel **Services** (one project, one domain, path-based
routing). The API cannot run on Vercel — it needs a JVM, a long-lived Postgres
connection and Flyway migrations at boot, so it lives on Render.

Deploy in this order: **Vercel first** (to get the origin the API must allow via
CORS), **then Render**, **then** point the frontends at the API URL and redeploy.
Vercel inlines `NEXT_PUBLIC_*` values at build time, so that last redeploy is
mandatory, not optional.

---

## 1. Vercel — storefront + dashboard

1. **Add New → Project → Import** the GitHub repo, with **Root Directory = the
   repository root** (not `storefront/` or `dashboard/` — `vercel.json` at the
   root defines both services).
2. Framework preset: **Next.js** (Vercel reads the two `services` entries).
3. Before the first deploy, add **Environment Variables** (Production, and
   Preview if you use PR previews):

   | Variable | Value | Notes |
   |---|---|---|
   | `NEXT_PUBLIC_API_URL` | `https://meethybridhub-api.onrender.com` | Fill in the real Render URL in step 3; a placeholder is fine for the first build |
   | `NEXT_PUBLIC_STORE_SLUG` | `meethybridhub` | Must match a store row in the DB (see step 4) |
   | `NEXT_PUBLIC_PAYMENTS_ENABLED` | `false` | `true` only once Korapay is wired |

   Both services share the project's environment variables. A build **fails on
   purpose** if `NEXT_PUBLIC_API_URL` is missing, instead of shipping a bundle
   that calls `http://localhost:8080`.
4. Deploy. You get one domain serving:

   - `/` → storefront
   - `/dashboard` → dashboard (the dashboard app has `basePath: "/dashboard"`)

   Confirm both loads, then note the domain — it is the CORS origin for the API.

> **If Services is unavailable on your plan/region**, deploy two separate Vercel
> projects instead: same repo, **Root Directory** `storefront` and `dashboard`.
> `dashboard/next.config.ts` keeps `basePath: "/dashboard"`, so the dashboard
> will live at `<dashboard-project>.vercel.app/dashboard`. Everything else in
> this guide is unchanged.

## 2. Render — API + Postgres (Blueprint)

1. **Render Dashboard → New → Blueprint → select the repo.** Render reads
   `render.yaml` and shows the resources it will create: `meethybridhub-api`
   (Docker web service) and `meethybridhub-db` (Postgres 16).
2. Fill the prompted variables (`sync: false` in the Blueprint, so they are
   asked once at creation):

   | Prompt | Value |
   |---|---|
   | `CORS_ALLOWED_ORIGINS` | `https://<your-vercel-domain>` — exactly as the browser sends it, no trailing slash. Comma-separate extra origins (e.g. a preview domain or custom domain). |
   | `APP_BASE_URL` | Leave blank for now; it falls back to Render's own `RENDER_EXTERNAL_URL`. Set it only when the API is served from a custom domain. |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | Optional. Blank = the app boots and registration works, but verification emails fail to send. Gmail requires an **app password**. |

   Render generates `JWT_SECRET` and `PAYMENT_WEBHOOK_SECRET` itself
   (`generateValue: true`); they stay stable across deploys.
3. **Apply** and watch the build. The Dockerfile builds the jar with the Maven
   wrapper, then runs it as a non-root user. First boot: Flyway applies
   `V2…V10`, Hibernate validates the schema (`ddl-auto: validate` — it refuses to
   start on drift), and the service goes healthy once `/actuator/health` answers.
4. Copy the service URL (`https://<service>.onrender.com`).

`APP_ENV=prod` is set in the Blueprint, so `StartupSecretsValidator` aborts the
boot if `JWT_SECRET`, `PAYMENT_WEBHOOK_SECRET` or `DB_PASSWORD` are missing or
still at their committed dev defaults. A boot failure with
`Refusing to start with APP_ENV=prod: …` means one of them did not reach the
container.

## 3. Close the loop on Vercel

Back in Vercel → Settings → Environment Variables, set `NEXT_PUBLIC_API_URL` to
the Render URL from step 2.4 (no trailing slash — it is stripped anyway), then
**Deployments → … → Redeploy** (uncheck "use existing build cache" if you want a
clean rebuild). The value is baked into the browser bundle at build time.

## 4. Bootstrap data (empty database)

The schema exists, but there is no seed data. New accounts are created
`PENDING` / `email_verified = false`, and the JWT filter **rejects tokens from
unverified accounts** (`isEnabled()` is false) — so activating the account is a
required step, not a formality.

```bash
API=https://meethybridhub-api.onrender.com

# 1. Create an account (roles: CUSTOMER, status: PENDING)
curl -sX POST "$API/api/v1/auth/register" -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"<strong-password>","fullName":"Your Name"}'
```

**2a. If `MAIL_*` is configured:** open the link in the verification email
(`GET /api/v1/auth/verify?token=…`).

**2b. Without SMTP** (nothing was sent), activate from the Render Dashboard
(**database → Connect → PSQL**) and log in again to get a usable token:

```sql
UPDATE users SET email_verified = TRUE, status = 'ACTIVE' WHERE email = 'you@example.com';
```

```bash
# 3. Log in for a token
curl -sX POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"<strong-password>"}'

# 4. Register the store (grants STORE_OWNER)
curl -sX POST "$API/api/v1/stores" -H 'Content-Type: application/json' \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"name":"My Store","description":"First store"}'
# → {"id":1,"name":"My Store","slug":"my-store",...}
```

The **slug is generated from the name** (lowercase, hyphenated, de-duplicated);
it is not client-chosen. Set Vercel's `NEXT_PUBLIC_STORE_SLUG` to the returned
value (e.g. `my-store`) and redeploy — the dashboard and storefront send it as
`X-Store-Slug` to resolve the tenant. Without it the API answers
`400 No store context`.

There is no self-service way to become `ADMIN` yet — roles are a
comma-separated column. Promote the account from the same PSQL session:

```sql
UPDATE users SET roles = 'CUSTOMER,ADMIN' WHERE email = 'you@example.com';
```

Log in again afterwards: roles are read from the database on each request, but
the dashboard caches the profile it fetched at login.

## 5. Verify the deployment

```bash
API=https://meethybridhub-api.onrender.com
WEB=https://<your-vercel-domain>

curl -s $API/actuator/health            # {"status":"UP"}  (db component included)
curl -si $API/api/v1/ping | head -1     # 200
curl -si $API/swagger-ui.html | head -1 # 200 (or a redirect — docs are public)

# CORS: the browser origin must be echoed back
curl -si -X OPTIONS "$API/api/v1/auth/login" \
  -H "Origin: $WEB" -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type' | grep -i access-control-allow-origin

# Both frontends
curl -s -o /dev/null -w '%{http_code}\n' $WEB/
curl -s -o /dev/null -w '%{http_code}\n' $WEB/dashboard

# Tenant resolution (the dashboard sends this header for you)
curl -s "$API/api/v1/stores/me" -H "Authorization: Bearer <accessToken>" \
  -H "X-Store-Slug: my-store"
```

Then in the browser: log in on `/dashboard/login`, and confirm the network tab
calls the `onrender.com` origin (not `localhost`) with no CORS errors.

---

## Operational notes

- **Free tier behavior.** Free web services sleep after ~15 minutes idle; the
  next request pays a 30–60s cold start. Free Postgres instances are also
  **deleted after 30 days** unless upgraded. Bump `plan` in `render.yaml`
  (`starter`) for anything beyond a demo.
- **Config lives in `render.yaml`.** Re-syncing the Blueprint applies the file;
  variables added only in the dashboard survive, but `sync: false` prompts are
  only asked during the *initial* creation — add new secrets manually later.
- **Redis is not deployed.** `application.yml` excludes Redis auto-configuration,
  so the Phase 9 cache layer stays dormant. Add a Render Key Value instance and
  drop the `spring.autoconfigure.exclude` entries when that layer lands.
- **Custom domains.** API: add `api.yourdomain.com` under the Render service's
  Custom Domains (CNAME → `*.onrender.com`), then set `APP_BASE_URL` to it and
  add the frontend origin(s) to `CORS_ALLOWED_ORIGINS`. Frontend: add the apex
  domain to the Vercel project. Wildcard store subdomains
  (`*.yourdomain.com` → `STORE_BASE_DOMAIN`) need a wildcard DNS record on
  Vercel — do that when per-store storefronts go live, not before.
- **Preview deployments** get their own `*.vercel.app` origins. CORS matches
  exact origins, so a preview build can render but its API calls are blocked
  until you add that origin to `CORS_ALLOWED_ORIGINS` (or test against
  production directly).
- **Rotating `JWT_SECRET`** logs every user out (all signed tokens become
  invalid). Rotating `PAYMENT_WEBHOOK_SECRET` breaks in-flight webhooks.
- **Logs/metrics.** Render service → Logs for the app; `/actuator/health`,
  `/actuator/metrics`, `/actuator/prometheus` are exposed for monitoring. Add a
  pinger against `/actuator/health` if you want to keep a free instance warm —
  note that this uses your free instance hours.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Render deploy health check fails, logs show `Connection refused` or Flyway retries | The DB env vars did not resolve. Check `DB_HOST`/`DB_PORT`/`DB_NAME` on the service; the JDBC URL is composed from them (a Render `postgres://` string is **not** a JDBC URL). |
| `Refusing to start with APP_ENV=prod: …` | `JWT_SECRET`, `PAYMENT_WEBHOOK_SECRET` or `DB_PASSWORD` missing/blank/dev-default. Re-check the service env vars. |
| `Schema-validation: missing table …` at boot | Flyway did not run against the DB the app connected to (wrong `DB_NAME`), or the DB volume was reset while the app expected the old schema. |
| Browser console: `blocked by CORS policy` | `CORS_ALLOWED_ORIGINS` does not contain the exact origin (scheme + host + port, no trailing slash). Edit it on Render — env changes restart the service, no rebuild needed. |
| Frontend calls `http://localhost:8080` | `NEXT_PUBLIC_API_URL` was missing at build time. Set it in Vercel and **redeploy** (build-time inlining). |
| `/dashboard` 404s on Vercel | The project was imported with a sub-directory root. Re-import with the repo root so `vercel.json`'s services + rewrites apply. |
| Dashboard loads but every store call returns `400 No store context` | `NEXT_PUBLIC_STORE_SLUG` does not match a store slug. Copy the `slug` from the `POST /api/v1/stores` response into Vercel and redeploy. |
| Login works, then every authenticated call returns `403 Access denied` | The account is still `PENDING` / unverified — the JWT filter ignores tokens for disabled accounts. Verify the email (or run the activation SQL in step 4). |
| Postgres connection errors under load | Free Postgres allows 50 connections; the Hikari default is 10 per instance. Keep `numInstances` low, or enable `connectionPool: pgbouncer` in `render.yaml`. |
