# UML Diagrams — MeethybridHub

Every diagram exists in **two formats**:

| | Format | Where | How to view |
|---|---|---|---|
| 🖥️ **draw.io** (editable) | `.drawio` XML | this folder | [app.diagrams.net](https://app.diagrams.net) → drag & drop the file, or VS Code *Draw.io Integration* extension |
| 📖 **Mermaid** (GitHub-native) | `.md` with ```` ```mermaid ```` blocks | [`mermaid/`](mermaid/) | rendered automatically by GitHub in the file viewer / README |

> The **draw.io files are the editable source of truth**. The Mermaid versions are for quick viewing on github.com — regenerate or update them by hand when the schema/API changes.

## Index

| File (draw.io) | Mermaid | Type | Contents |
|---|---|---|---|
| [`01-class-diagram.drawio`](01-class-diagram.drawio) | [01-class-diagram.md](mermaid/01-class-diagram.md) | Class | JPA domain model: `User`, `AppUser`, `Store`, `StoreDomain`, `TenantEntity`, token entities, `LoginAttempt`, enums (`Role`, `UserStatus`, `StoreStatus`, `Purpose`), with relationships |
| [`02-er-diagram.drawio`](02-er-diagram.drawio) | [02-er-diagram.md](mermaid/02-er-diagram.md) | ER | Database schema (Flyway V2–V6): `users`, `stores`, `store_domains`, `email_verification_tokens`, `password_reset_tokens`, `login_attempts`, `audit_log` — columns, PKs, FKs |
| [`03-component-diagram.drawio`](03-component-diagram.drawio) | [03-component-diagram.md](mermaid/03-component-diagram.md) | Component | Layered architecture: filter chain → controllers → services → data, plus `common/` and external systems (PostgreSQL, SMTP, client) |
| [`04-sequence-login.drawio`](04-sequence-login.drawio) | [04-sequence-login.md](mermaid/04-sequence-login.md) | Sequence | `POST /api/v1/auth/login` flow: rate limit → authenticate → tenant claims → access/refresh tokens |
| [`05-sequence-request.drawio`](05-sequence-request.drawio) | [05-sequence-request.md](mermaid/05-sequence-request.md) | Sequence | Authenticated request lifecycle: JWT filter → `StoreFilter` tenant resolution → controller → service → tenant ownership check |
| [`06-use-case-diagram.drawio`](06-use-case-diagram.drawio) | [06-use-case-diagram.md](mermaid/06-use-case-diagram.md) | Use case | Actors (Guest, User, Store Owner, Admin) and their use cases against the `/api/v1` API |

## Keeping the two formats in sync

The `.drawio` files are the editable source of truth; the `mermaid/*.md` views are kept in step with them by hand. **`sync_diagrams.py`** (Python 3, no dependencies) checks they haven't drifted and can render a local preview:

```bash
# Validate everything: XML structure, coverage (every diagram in both formats),
# and class/table name consistency for the class + ER diagrams.
python3 sync_diagrams.py check            # exit 0 = all good, 1 = problems
python3 sync_diagrams.py check --verbose  # per-file detail

# Build docs/uml/preview.html rendering all Mermaid diagrams in one page.
python3 sync_diagrams.py preview
```

> When you change a `.drawio` file (add an entity, rename a class, add a table), update the matching `mermaid/NN-*.md` too, then run `check` to confirm. Run it in CI on every push to catch drift automatically.

## Quick view links (draw.io)

Click a diagram file on GitHub, copy its **raw URL**, then open:

```
https://app.diagrams.net/#U<raw-github-url>
```

Example: `https://app.diagrams.net/#Uhttps://raw.githubusercontent.com/<you>/meethybridhub/main/docs/uml/01-class-diagram.drawio`

## Legend

- 🔑 = primary key · 🔗 = foreign key
- Blue = JPA entities / Spring components · Green = enums / service layer · Red = data layer · Purple = cross-cutting
- Dashed UML edge = dependency/`extends` · solid = association/composition

> Diagrams are regenerated documentation — if the schema or API changes, update the matching `.drawio` files (see `src/main/resources/db/migration/` and the controller/service sources).
