# Elicit FHHS — Claude / AIUP Context

This project follows the **AI Unified Process (AIUP)**
([unifiedprocess.ai](https://unifiedprocess.ai/)). Treat the artifacts under
`docs/` as the source of truth for *what* this system is supposed to do; the
code under `src/` is the implementation that AIUP regenerates and refactors
around them.

## Stack

- Java 25, Quarkus 3.39.2, Maven build
- No UI — FHHS is a headless REST backend (unlike the companion Survey/Admin
  apps, which use Vaadin Flow)
- Hibernate ORM with Panache (JPA) — **not jOOQ**
- PostgreSQL, sharing the `survey` schema owned by the Elicit Survey app
  (Flyway migrations under `src/main/resources/db/migration`)
- Docker for deployment
- OpenTelemetry, SmallRye Health, Micrometer/Prometheus metrics
- Calls an external R/Kinship2-based pedigree-drawing HTTP service

## MCP Servers and Skills

Use the configured MCP servers and their skills instead of raw Maven, web
search, or recalled API knowledge.

**Quarkus (`quarkus-agent` plugin)** — this is an **existing project**: start
with `quarkus_update`, then `quarkus_skills` for every extension you are about
to touch; never `quarkus_create`. Look up Quarkus configuration and APIs with
`quarkus_searchDocs`, not Context7 or web search. Manage dev mode with
`quarkus_start` / `quarkus_stop` / `quarkus_status` / `quarkus_logs`; reload
after code changes via `quarkus_callTool` → `devui-logstream_forceRestart`,
and do a full stop/start after any `pom.xml` change. Run tests through
`quarkus_callTool` → `devui-testing_runTests` (or `devui-testing_runTest`
with a class name), not `mvn test`, and never `mvn clean` while dev mode is
running. `quarkus_searchTools` lists the Dev MCP tools on the running app;
re-run it after adding or removing an extension. The full workflow
(extension-first rule, error recovery) is in [AGENTS.md](AGENTS.md).

**Vaadin (`vaadin-skills` plugin)** — not applicable. FHHS is headless REST;
do not load the Vaadin skills or MCP tools for this module.

**IntelliJ IDEA (`idea` MCP server)** — `.run/FHHS.run.xml` is the
Quarkus dev-mode run configuration; launch it with
`execute_run_configuration` (see `get_run_configurations`) rather than
hand-rolling `mvn quarkus:dev`. When the project is open in the IDE, prefer
`search_symbol`, `get_symbol_info`, `analyze_calls`, `get_file_problems`,
`lint_files`, `rename_refactoring`, `reformat_file`, and `build_project` over
grep-and-edit. The database tools (`list_database_connections`,
`introspect_schema`, `execute_sql_query`, `preview_table_data`) can inspect
the local `survey` schema; the `xdebug_*` tools set breakpoints and step
through a running debug session.

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
