# Coverage gaps & test tracker

Snapshot of the under-covered classes in `meethybridhub`, ranked by **impact per effort**,
with a checkbox tracker so each gap can be ticked off as tests land.

> **Status:** last full `./mvnw verify` — **103 tests, 0 failures**, all JaCoCo gates green.
> **Numbers:** measured from `target/site/jacoco/jacoco.csv` (regenerate with `./mvnw verify`;
> browse the HTML at `target/site/jacoco/index.html`).

## Current per-package state

| Package | Line | Branch | Comment |
|---|---|---|---|
| `common.api` | **100%** | **100%** | exception handlers fully covered (P0 done) |
| `common.exception` | 100% | 100% | trivial |
| `common.config` | 97.6% | 83.3% | 1 line + 1 branch in `JpaAuditingConfig` |
| `identity` | **90.6%** | 71.2% | `UserService` is the biggest remaining gap |
| `store` | 85.7% | 61.3% | filter/domain gaps |
| `ping` | 100% | 100% | |

## How to use this tracker

- `[x]` = tests written and green; `[ ]` = open.
- Priorities: **P1** (security-critical) → **P2** (biggest absolute gap) → **P3** (store/tenant) → **P4** (trivial DTO/entity boilerplate — lowest value, mostly covered by the per-package floors anyway).
- "Pure unit" means no Spring context: construct the class directly, mock dependencies with Mockito (available via `spring-boot-starter-test`).

---

## ✅ P0 — DONE

- [x] **GlobalExceptionHandler** (`common.api`) — 55% → **100% line / 100% branch**
  - `GlobalExceptionHandlerTest` (18 tests): all 17 handlers + the `getSupportedHttpMethods() == null` branch.
- [x] **UserDetailsServiceImpl** (`identity`) — 50% → **100% / 100%**
  - `UserDetailsServiceImplTest` (6 tests): `loadUserForAuthentication` rejection branches (unverified email, non-ACTIVE status, unknown email) + `loadUserByUsername` paths.
- [x] **DefaultEmailService** (`identity`) — 42.5% → **100% / 80%**
  - `DefaultEmailServiceTest` (5 tests): SMTP-configured send path (mocked `JavaMailSenderImpl` via `ReflectionTestUtils`), both email types' content, the `MessagingException → IllegalStateException` catch, dev-mode logging, constructor short-circuit.
  - Remaining 2 branches are the `host != null` / `username != null` null-guards — **unreachable** (Spring `@Value` never injects null). No further action needed.

---

## 🟠 P1 — JWT core (security-critical)

- [x] **JwtService** (`identity`) — 76.5% → **100% line / 87.5% branch**
  - [x] `getRemainingValidityMinutes` (5/5 lines — **entirely untested**) — fresh/near-expiry bands + the `isBefore(now) → 0` guard (via a stub, since jjwt throws `ExpiredJwtException` for expired tokens at parse time)
  - [x] `validateToken` (2/4): valid, different user, expired, malformed, wrong secret
  - [x] `getSigningKey` (2/4): normal + short-secret `IllegalStateException`
  - [x] `passwordVersionMatches` (1/4): current version, version bumped after issuance, missing claim, non-`AppUser` principal
  - [x] `generateAccessToken` / `generateRefreshToken` (1 missed line each) — subject/expiration round-trips + extra claims
  - `JwtServiceTest` (16 tests) — pure unit with a fixed test secret via `ReflectionTestUtils`. Remaining 2 branches are the non-`AppUser` defensive path in `baseClaims`.

- [ ] **JwtAuthenticationFilter** (`identity`) — **83.0% / 57.9%**; 8 missed lines, 16 missed branches
  - [ ] `doFilterInternal` negative paths (6 lines): invalid/expired token, missing token
  - [ ] `shouldNotFilterErrorDispatch` / `shouldNotFilterAsyncDispatch` (1 line each)
  - Suggested: `MockHttpServletRequest`/`MockHttpServletResponse` + mocked `JwtService`/`UserDetailsService` — no Spring Security context needed.

---

## 🟡 P2 — service level (biggest absolute gap)

- [ ] **UserService** (`identity`) — **86.4% / 68%**; 24 missed lines, 16 missed branches (most absolute misses in the codebase)
  - [ ] `updateProfile` (3/3 — **entirely untested**)
  - [ ] `listUsers` (4) — admin list path has no tests
  - [ ] `resendVerificationEmail` (4)
  - [ ] `validatePassword` (4)
  - [ ] `register` (3)
  - [ ] `requestPasswordReset` (2), `confirmPasswordReset` (1), `changePassword` (1)
  - [ ] `normalizeRoles` (2)
  - Suggested: mocked `UserRepository` + `PasswordEncoder`; pure unit tests for each service method.

- [ ] **UserController** (`identity`) — **83.3%**; `updateProfile` endpoint (3 lines)
  - Suggested: `@WebMvcTest(UserController.class)` with mocked service.

---

## 🔵 P3 — store / tenant

- [ ] **StoreFilter** (`store`) — **87.1% / 59.5%**; 8 missed lines, 17 missed branches
  - [ ] `resolveFromHeader` when the store doesn't exist (5)
  - [ ] `resolveFromJwt` when the token has no `storeId` (2)
  - [ ] `extractSubdomain` edge (1)
  - Suggested: `MockHttpServletRequest`-based, like the JWT filter.

- [ ] **StoreService** (`store`) — **93.9% / 61.1%**
  - [ ] `addDomain` (1), `uniqueSlug` (2), `getCurrentTenantStore` missing-store lambda (1)

- [ ] **StoreDomain** (`store`) — **52.4%** (10 missed lines)
  - Mostly setters/`toString`; low value unless `setPrimary`/`setVerified` carry normalization logic worth locking down.

- [ ] **TenantContext** (`common`) — **90%**; `getStoreId` (1 line) — thread-local getter.

- [ ] **AppUser** (`identity`) — **100% line / 50% branch**
  - [ ] `isAccountNonLocked` SUSPENDED branch (1 branch) — one-liner: add a suspended-user case to `UserDetailsServiceImplTest`.

---

## ⚪ P4 — trivial (DTO/entity boilerplate)

Lowest value — the per-package floors already tolerate these; only worth doing while touching the class for other reasons.

- [ ] **Store** (`store`) — 68.0%: setters + `toString`
- [ ] **User** (`identity`) — 82.2% / 62.5%: `setEmail`, `setFullName`, `setLastLoginAt`, `getLastLoginAt`, `toString`
- [ ] **LoginAttempt** (`identity`) — 57.9%: 8 getters
- [ ] **EmailVerificationToken** / **PasswordResetToken** (`identity`) — 81.2%: `getId`/`getUsedAt`/`getCreatedAt`
- [ ] **Role** (`identity`) — 90% / 50%: `isValid` (1 line, 2 branches)
- [ ] **UpdateProfileRequest** — 0%: 1 missed line (DTO)
- [ ] **SchedulingConfig** — 0%: `<init>` (1 line)
- [ ] **MeethybridHubApplication** — 33.3%: `main` (2 lines) — fine to leave; context is loaded by the `@SpringBootTest` suite.

---

## Guardrails (why this matters)

- **JaCoCo per-package floors** (`pom.xml`, `verify` phase — build fails below these):
  - bundle / `identity` / `store`: **line ≥ 80%, branch ≥ 55%**
  - `common`: **line ≥ 55%** (no branch floor — `GlobalExceptionHandler` used to drag it down; now 100%)
- **Codecov** (`codecov.yml`): project ≥ 80% (1% tolerance), patch ≥ 80% (5% tolerance) — PR status checks + trend graph.
- Doing **P1 + P2** lifts `identity` from 88.7% toward ~95%+ and closes the security-critical token paths.

## Regenerating these numbers

```bash
./mvnw verify                          # runs tests + jacoco report + coverage gates
# per-class CSV (sorted by line %):
python3 -c "
import csv
rows = list(csv.DictReader(open('target/site/jacoco/jacoco.csv')))
for r in sorted(rows, key=lambda r: int(r['LINE_COVERED']) / max(int(r['LINE_COVERED']) + int(r['LINE_MISSED']), 1)):
    print(f\"{r['CLASS'].split('.')[-1]:28s} {int(r['LINE_COVERED']) / (int(r['LINE_COVERED']) + int(r['LINE_MISSED'])) * 100:5.1f}%\")
"
# or open the HTML report:
# target/site/jacoco/index.html
```
