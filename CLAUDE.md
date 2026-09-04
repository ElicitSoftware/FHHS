# Elicit FHHS — Claude / AIUP Context

This project follows the **AI Unified Process (AIUP)**
([unifiedprocess.ai](https://unifiedprocess.ai/)). Treat the artifacts under
`docs/` as the source of truth for *what* this system is supposed to do; the
code under `src/` is the implementation that AIUP regenerates and refactors
around them.

## Stack

- Java 25, Quarkus 3.37.x, Maven build
- No UI — FHHS is a headless REST backend (unlike the companion Survey/Admin
  apps, which use Vaadin Flow)
- Hibernate ORM with Panache (JPA) — **not jOOQ**
- PostgreSQL, sharing the `survey` schema owned by the Elicit Survey app
  (Flyway migrations under `src/main/resources/db/migration`)
- Docker for deployment
- OpenTelemetry, SmallRye Health, Micrometer/Prometheus metrics
- Calls an external R/Kinship2-based pedigree-drawing HTTP service

When using the Quarkus Agent MCP tools, this is an **existing project** — start
with `quarkus_update` and `quarkus_skills` per those tools' instructions. See
[AGENTS.md](AGENTS.md) for the full Quarkus-agent workflow (extension-first
development, hot reload, test running, error recovery).

## AIUP Workflow

The plugin `aiup-core` provides these methodology skills. Run them roughly in
this order; review and hand-edit each artifact before continuing.

1. `/requirements` — generates `docs/requirements.md` from `docs/vision.md`.
2. `/entity-model` — produces a Mermaid ER diagram and attribute tables.
3. `/use-case-diagram` — creates a PlantUML diagram with stable UC IDs.
4. `/use-case-spec UC-XXX` — writes detailed per-use-case specifications.
5. *(implementation + tests — stack-specific; we are not using
   `aiup-vaadin-jooq` because this project uses Hibernate/Panache, not jOOQ,
   and has no Vaadin UI to begin with.)*

**Brownfield entry point:** `/reverse-engineer` recovers AIUP artifacts from
the existing source. This repo's `docs/use_cases.puml`, `docs/use_cases/`, and
`docs/entity_model.md` were produced this way, since the codebase predates
AIUP adoption; `docs/vision.md` was hand-authored as the AIUP seed document.

## Git Commits

- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages.

## Working Agreements

- Re-run upstream skills when requirements change so downstream artifacts
  (entity model, use-case specs) stay consistent.
- Tests must be traceable to a use case — reference the `UC-XXX` ID in test
  names or comments.
- The `docs/` folder is institutional memory. Commit it to version control.
- Do not mix in jOOQ-specific patterns; data access is Hibernate/Panache.
- FHHS has no human users of its own — its actors are the Survey Platform
  (which triggers report generation as a post-survey action) and its own
  internal Scheduler. Don't invent a human "user" actor for it.

## Reference

- Vision: `docs/vision.md`
- Companion apps: [Survey](https://github.com/ElicitSoftware/Survey) (also
  AIUP), [Admin](https://github.com/ElicitSoftware/Admin) (also AIUP)
- Marketplace: https://github.com/AI-Unified-Process/marketplace
