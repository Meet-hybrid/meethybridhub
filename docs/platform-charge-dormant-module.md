# Dormant Platform-Charge Module — Handoff Notes

> **Status:** IMPLEMENTED (dormant) — Aug 7, 2026. The module now lives in
> `src/main/java/com/meethybridhub/billing/` behind `PLATFORM_CHARGE_ENABLED=false`.
> This doc remains the memory for **waking it up** (see "Wake-up instructions").

## What was asked

A **dormant** "script" that charges a flat amount per transaction as a **platform
charge**, behind the scenes. It must **sleep until activated**.

## Decisions (user-confirmed via ask_user, Aug 6 2026)

| Question | Decision |
|---|---|
| Which codebase? | **meethybridhub** (Java / Spring Boot 3.5 / Java 21) |
| Charge model? | **Flat fee per transaction** (no percentage) |
| Sleep switch? | **Config flag in the app** (env var, default off) |

## Codebase facts gathered (don't re-research)

- **No transaction / order / payment entity exists yet** — "Orders + Payments" is a
  future phase. The module must be built as a dormant seam for that phase.
- **Migrations:** `V2__identity.sql` … `V7__audit_log_ip_to_varchar.sql` exist →
  next is **`V8__platform_charges.sql`** (V7 was taken by the audit-log IP
  change that shipped with the AuditLog entity).
- **Scheduling:** `@EnableScheduling` lives in `config/SchedulingConfig.java`
  (`app.scheduling.enabled`, `matchIfMissing=true`; **disabled in test profile**).
  Style reference: `identity/TokenCleanupService.java` — `@Scheduled(cron = "${app.token-cleanup.cron:0 0 3 * * *}")`.
- **Entity style reference:** `store/Store.java` — jakarta persistence,
  `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate`/`@LastModifiedDate`.
- **Config style:** `src/main/resources/application.yml` — `key: ${ENV_VAR:default}`,
  sections for `jwt`, `store`, `app`, `auth`, `mail`. No secrets committed.
- **JaCoCo per-package floors** (pom.xml): `identity*` 80% line / 55% branch,
  `store*` 80/55, `common*` 55% line only; BUNDLE 80/55. A new `billing` package
  needs **its own floor rule** or it's only held by the bundle floor.
- **Test conventions:** pure unit tests (JUnit 5 + Mockito + AssertJ), no Spring
  context; `ReflectionTestUtils.setField` for `@Value`-injected fields (see
  `JwtServiceTest`). `@SpringBootTest` only for integration tests.
- **Git:** work lives on branch `ci-coverage-docs` (PR #5 open with the coverage
  test work). `cli.py` contains a hardcoded API key — **never commit it** (stays
  untracked). `venv/` is gitignored.

## Implementation blueprint (executed Aug 7, 2026)

### New files
1. `src/main/resources/db/migration/V8__platform_charges.sql` — V7 was taken by
   the audit-log IP change, so the table landed as **V8**:
   - `platform_charges` table: `id BIGSERIAL PK`, `transaction_ref VARCHAR(100) NOT NULL UNIQUE`,
     `transaction_amount NUMERIC(12,2) NOT NULL`, `charge_amount NUMERIC(12,2) NOT NULL`,
     `currency VARCHAR(3) NOT NULL DEFAULT 'NGN'`, `status VARCHAR(20) NOT NULL DEFAULT 'PENDING'`
     (`PENDING`/`COLLECTED`/`FAILED`), `created_at`/`updated_at TIMESTAMPTZ`.
2. `src/main/java/com/meethybridhub/billing/PlatformCharge.java` — JPA entity matching the migration.
3. `src/main/java/com/meethybridhub/billing/PlatformChargeRepository.java`
   - `findByTransactionRefIn(Collection<String>)`, `existsByTransactionRef(String)`, `countByStatus(...)`.
4. `src/main/java/com/meethybridhub/billing/ChargeableTransactionSource.java` — seam interface
   the future Orders/Payments module will implement:
   `List<ChargeableTransaction> findSettledTransactionsBefore(Instant cutoff);`
5. `src/main/java/com/meethybridhub/billing/ChargeableTransaction.java` — record
   `(String transactionRef, BigDecimal amount)`.
6. `src/main/java/com/meethybridhub/billing/PlatformChargeService.java` — the core:
   - `@Value` config: `platform-charge.enabled` (default `false` — **the sleep switch**),
     `platform-charge.flat-fee` (default `0`), `platform-charge.currency` (default `NGN`),
     `platform-charge.sweep-cron` (default `0 0 3 * * *`).
   - `boolean isEnabled()`
   - `Optional<PlatformCharge> charge(String transactionRef, BigDecimal amount)`:
     disabled → log "dormant, skipping" and return empty; enabled → compute flat fee,
     idempotent via unique `transaction_ref`, persist + audit log.
   - `@Scheduled(cron = "${platform-charge.sweep-cron:0 0 3 * * *}")` sweep: gated on
     `enabled`; iterate injected `List<ChargeableTransactionSource>` (empty list today →
     log once, no-op), filter refs already charged, charge the rest. `@Transactional`.
7. `src/test/java/com/meethybridhub/billing/PlatformChargeServiceTest.java` — unit tests:
   - dormant (enabled=false): `charge()` no-op, sweep no-op
   - enabled: flat fee persisted (e.g. ₦50 on ₦5,000), idempotent (same ref twice → 1 row)
   - sweep with a mock `ChargeableTransactionSource` → charges only uncharged refs
   - `isEnabled()` both ways

### Edits
8. ✅ `src/main/resources/application.yml` — block added (no `application-test.yml`
   mirror needed: defaults are dormant and unit tests set fields via
   `ReflectionTestUtils`):
   ```yaml
   platform-charge:
     enabled: ${PLATFORM_CHARGE_ENABLED:false}
     flat-fee: ${PLATFORM_CHARGE_FLAT_FEE:0}
     currency: ${PLATFORM_CHARGE_CURRENCY:NGN}
     sweep-cron: ${PLATFORM_CHARGE_SWEEP_CRON:0 0 3 * * *}
   ```
9. ✅ `pom.xml` — JaCoCo PACKAGE rule added: `com.meethybridhub.billing*` ≥ 80% line / 55% branch.
10. ⏭️ `README.md` — "Dormant features" section deliberately deferred (avoids
   colliding with the open README PR); add when the module is woken up.

### Extra (added during implementation)
- `identity/AuditEventType` gained `PLATFORM_CHARGE_RECORDED`; successful charges
  are recorded to the audit trail via `AuditLogService` (billing → identity).
- `charge()` catches `DataIntegrityViolationException` from the unique
  `transaction_ref` as the concurrency-safe idempotency guard (exists-then-save
  alone has a race).

### Wake-up instructions (for ops)
Set `PLATFORM_CHARGE_ENABLED=true` + `PLATFORM_CHARGE_FLAT_FEE=<amount>` in prod env
(and implement `ChargeableTransactionSource` once Orders/Payments land). Nothing else
changes — the sweep and inline `charge()` calls start recording automatically.

## Verification on resume
```bash
./mvnw -B test -Dtest='PlatformChargeServiceTest'      # fast loop
./mvnw -B verify                                       # full suite + JaCoCo gates (~103+ tests)
# coverage: python3 -c "import csv; ..." on target/site/jacoco/jacoco.csv
```

## Open questions for the user (ask before/while implementing)
- What marks a transaction as chargeable — every settled transaction, or exclusions
  (e.g. no charge on refunds/zero amounts)?
- Should the fee be deducted from the customer's payment or the store owner's payout?
- **Transparency:** platform fees must be disclosed to merchants/users (terms + UI) —
  most jurisdictions require it; silent deductions can be illegal. Recommend surfacing
  the fee in the future Orders/Payments flow.
