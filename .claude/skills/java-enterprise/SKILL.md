---
name: java-enterprise
description: Engineering standards for this Spring Boot / Vaadin / Spring AI codebase — build and quality gates, layering and feature packaging, configuration and secrets, persistence and Flyway, the LLM boundary, UI implementation against the approved mockups in docs/ui, testing, observability, and the definition of done. Load before writing or reviewing any Java, Gradle, SQL migration, Vaadin view, theme CSS, or configuration in this repo.
---

# Java Enterprise Standards

Applies to every change in this repo. Scope lives in `docs/0-design-arena.md`; the approved UI
lives in `docs/ui/ui/*.html`. Both are binding. This file governs *how* code is written, not
*what* is built. Where any two of the three conflict, stop and ask.

This codebase is also public teaching material: prefer the clear implementation over the
clever one, and comment the **why** behind non-obvious decisions, never the what.

## Build

- `./gradlew` only — never a local `gradle`, `mvn`, or IDE build. Never edit `gradle/wrapper/*`
  to change versions; use `./gradlew wrapper --gradle-version=X`.
- Versions come from BOMs (Spring Boot, Spring AI, Vaadin). A version literal on a
  dependency line needs a comment justifying it.
- `./gradlew build` must stay green with zero warnings-as-noise. Compile with `-Xlint:all`
  and `-Werror`; suppress narrowly at the symbol with a reason, never project-wide.
- Quality gates run in `check` and must block, not warn: Spotless (format), Checkstyle or
  PMD (static analysis), JaCoCo coverage floor, and the test task. Do not lower a gate to
  make a build pass — fix the code or raise the issue.
- One command bootstraps a dev machine: `./gradlew bootRun` with Docker Compose support
  starting Postgres and Ollama. Keep it that way.
- Builds are reproducible and offline-safe: no snapshot dependencies, no network calls in
  tests other than Testcontainers, no machine-specific paths.
- **Generated code never mixes with written code.** Everything under `src/` is written by
  a person; generated output belongs in `build/` and is git-ignored. Where a tool fixes
  the location and it cannot be moved (Vaadin's `src/main/frontend/generated`, the npm
  project at the root), it is git-ignored and the reason is documented next to it. Never
  edit generated output.

## Layering

- Package by feature (`problem`, `quiz`, `review`, `ai`, `user`, `ui`, `config`), never by layer.
- Dependencies point one way: `ui` → service → repository. UI never touches repositories,
  entities, or the AI layer.
- Services own the transaction boundary (`@Transactional` on the service method). Repositories
  stay query-only; controllers and views stay orchestration-only.
- Entities do not leave their feature package. Cross-boundary data travels as records.
- Constructor injection, `final` fields, no field `@Autowired`, no `@Component` on anything
  that could be a plain object.

## Configuration and secrets

- No secret, key, or credential in the repo or in any committed config — env vars with an
  empty default (`${OPENAI_API_KEY:}`).
- Typed config only: `@ConfigurationProperties` records with `@Validated` constraints.
  No bare `@Value` for anything structural.
- Profiles are the switch for environment behaviour (`local`, `cloud`). The app must start
  with no paid API keys.

## Persistence

- Flyway owns the schema. Hibernate `ddl-auto` stays `validate` — never `update` or `create`.
- Migrations are versioned, forward-only, and immutable once committed. Fix a mistake with a
  new migration; never edit an applied one.
- Reference and seed data ship as their own migrations, clearly marked as seed.
- No `findAll()` on a growing table, no entity graph loaded lazily across a transaction
  boundary, no N+1 left in place once spotted.

## The LLM boundary

- All model access goes through the single interface in `ai/`. No other package imports a
  Spring AI type — this is a hard rule, and the main reason provider swapping stays trivial.
- Provider and model are chosen by configuration alone. Changing provider must require no
  code change.
- Prompts live in `src/main/resources/prompts/*.st` with a version constant that is persisted
  alongside every result. No prompt text in Java strings.
- Model output is structured into a Java record, never parsed ad hoc. Malformed output retries
  up to the configured limit, then fails explicitly with a recorded status.
- User-supplied text is data: delimit it and instruct the model to ignore instructions inside it.
- Every AI call has a timeout and a defined fallback. A model failure degrades the feature,
  never breaks the page.
- Deterministic work (scoring, validation, totals) is Java code. The model is for judgement
  and narrative only.

## UI

- The mockups in `docs/ui/ui/` (`Main`, `Quiz`, `QuizResults`, `Editor`, `Review`) are the
  approved design and are **binding** — layout, tokens, copy, states and accessibility
  attributes. Read the relevant mockup before building or changing a view. A deliberate
  deviation needs the user's agreement first; never redesign silently.
- `references/ui-contract.md` holds the extracted tokens, per-screen structure and
  accessibility rules. Read it alongside the mockup.
- The mockups are design-tool exports with inline styles. **Never copy their markup or
  inline styles.** Implement in Vaadin Flow; style through the theme.
- Design tokens are CSS custom properties in the Vaadin theme, defined once. No hex colour,
  font stack, radius or spacing literal in Java, in a view, or in a second stylesheet.
- Prefer Vaadin components over hand-rolled layout, and a shared component over a copy —
  the header, score tile, chip, score bar and finding card each exist once.
- Accessibility is not optional: 44px minimum target height, real labels and fieldsets,
  `role="status"` and `role="alert"` on async states, ARIA on progress bars, meaning never
  carried by colour alone, visible focus order.
- Responsive by the contract's rules: `auto-fit` card grids, flex panes that collapse,
  never a fixed pixel width that can overflow a narrow viewport.

## Documentation

- `docs/0-design-arena.md` is the primary document: scope, architecture, milestones. It
  carries the system diagrams and stays current as the design changes.
- Each milestone ends with a story in `docs/learnings/`, written to be published on
  Medium as it stands. The constraints are part of the deliverable:
  - **1500-1700 words.** Long enough to teach something, short enough to finish.
  - **Mermaid diagrams**, at least one architecture and one flow diagram per story,
    coloured with the product palette so the docs and the product look related.
  - **A short explanation of each technology** the first time it appears. Assume a
    competent engineer who has not used Spring AI, Vaadin, Flyway or Testcontainers.
  - **What went wrong and how it was fixed**, with the real error. The mistakes are the
    most useful part; never quietly omit one.
- Write for a reader outside the project. No internal shorthand, no "as discussed".

## Error handling

- No empty `catch`, no `catch (Exception)` that hides a bug, no exception swallowed to keep a
  flow alive without a log and a recorded state.
- Throw typed domain exceptions; map them to user-facing messages at the edge. Stack traces
  and provider errors never reach the user.
- Validate at the boundary (`@Valid`, explicit length and range caps) before any expensive or
  outbound call.

## Testing

- Every behaviour change ships with a test in the same commit. Bug fixes ship with the test
  that would have caught them.
- Fast unit tests for logic with no Spring context. Testcontainers for anything touching
  Postgres. A mocked chat model for AI paths, including the malformed-output case.
- Tests are deterministic: no `Thread.sleep`, no wall-clock dependence, no shared mutable
  state, no ordering assumptions.
- Assert on behaviour, not on implementation detail. Never weaken an assertion to make a test
  pass.

## Observability

- Log through SLF4J with parameterised messages. Never log secrets, API keys, prompts
  containing user data, or PII.
- Actuator health and info stay accurate; `info` surfaces the active AI provider and model.
- Record latency and outcome for every outbound model call.

## Definition of done

A change is done when all of these hold:

1. `./gradlew build` passes, including every quality gate.
2. Tests cover the new behaviour and the failure path.
3. No secrets, no `TODO` left as the actual implementation, no commented-out code.
4. UI work matches its mockup and uses theme tokens, with no inline styles or stray hex values.
5. Config changes are documented where a reader will look (README or the brief).
6. The commit is one concept with a message explaining why, not just what.

Report honestly: if a gate fails or a step was skipped, say so with the output. Do not claim
completion on partial work.
