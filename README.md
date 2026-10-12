# Design Arena

System design practice with AI feedback — and a worked example of building an AI
application in Java with Spring Boot, Spring AI and Vaadin.

Two ways to practise:

1. **Quiz mode** — answer multiple-choice questions about the design decisions in a
   problem, get a deterministic score, per-category breakdown and an explanation for every
   option, plus an AI-written narrative of the design your choices imply.
2. **Design review mode** — write a design in Markdown with an optional Mermaid diagram,
   and get it reviewed against a staff-level rubric: strengths, risks, gaps and the
   follow-up questions an interviewer would ask.

The project is built in public. Deeper material on system design and on building AI
applications in Java lives at [codefarm.in](https://codefarm.in).

> **Status: all five milestones complete.** The quiz scores deterministically and works
> signed out; the design review takes Markdown and an optional Mermaid diagram and reviews
> it against a staff-level rubric as structured output, with a real retry path when the
> model will not produce one. Accounts bring attempt history and a free-plan limit.
> Administrators can edit problems, questions and rubrics, and see which model is actually
> answering. Payments and vision support are the obvious next steps.

## Running it locally

You need **Docker** and nothing else — the JDK is downloaded by the build if your machine
does not have the right one.

```bash
./gradlew bootRun
```

That single command starts Postgres and Ollama from `compose.yaml`, waits for both to
report healthy, applies the Flyway migrations including the seed data, and serves the app
on <http://localhost:8080>.

Ollama pulls `llama3.1:8b` (about 5GB) in the background on first start. The app does not
wait for it, and no screen blocks on a model: AI results arrive asynchronously.

**On macOS, expect the model to be slow.** A container cannot reach the Mac's GPU, so the
model runs on CPU and a full design review takes a few minutes. That is survivable
because every model call is asynchronous, but for a faster loop run Ollama natively and
point the app at it:

```bash
brew install ollama && ollama serve
SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434 ./gradlew bootRun
```

```bash
./gradlew build     # compile, format check, static analysis, tests, coverage gate
./gradlew test      # tests only (starts a throwaway Postgres via Testcontainers)

# The eval set: runs sample designs through the real model and checks that a strong
# answer outscores a weak one. Slow, and skipped unless you ask for it.
./gradlew test -Devals=true --tests '*DesignReviewEvalTest*'
```

## Switching the LLM provider

Changing provider is configuration, never code. Every model call in the application goes
through one interface, so nothing outside the `ai` package knows which provider is active.

| Profile | Provider | Configuration |
|---|---|---|
| `local` (default) | Ollama, on your machine | `application-local.yaml` |
| `cloud` | OpenAI, Anthropic, or any OpenAI-compatible endpoint | `application-cloud.yaml` + env vars |

```bash
# Anthropic
SPRING_PROFILES_ACTIVE=cloud \
DESIGNARENA_AI_PROVIDER=anthropic \
ANTHROPIC_API_KEY=... \
DATABASE_URL=jdbc:postgresql://... DATABASE_USERNAME=... DATABASE_PASSWORD=... \
./gradlew bootRun

# Any OpenAI-compatible endpoint (Groq, Together, OpenRouter, ...)
SPRING_PROFILES_ACTIVE=cloud OPENAI_BASE_URL=https://api.groq.com/openai OPENAI_API_KEY=... ./gradlew bootRun
```

No API key is needed to run locally, and no secret is ever committed: every credential
comes from an environment variable.

## How the AI part works

*All of this is live as of milestone 3.*

- **One seam.** All model access goes through a single interface in
  `in.codefarm.designarena.ai`. No other package imports a Spring AI type, which is what
  makes the provider swap above a configuration change rather than a refactor.
- **Prompts are files, not strings.** They live in `src/main/resources/prompts/*.st`, each
  with a version that is stored next to every result, so you can always tell which prompt
  produced a given review.
- **Structured output.** A Java record defines the schema; Spring AI puts it in the prompt
  and maps the reply back. Nothing parses model output by hand. A reply is also checked
  for sense — scores inside 0-10, dimensions present — because valid JSON is not a valid
  review. Anything that fails is retried up to `designarena.ai.max-retries`, and then the
  evaluation is marked `FAILED` with a retry button rather than failing silently.
- **User text is data, not instructions.** A submission is wrapped in delimiters and the
  model is told to ignore any instructions inside it — the basic prompt-injection guard.
- **The model is not the scorer.** Quiz scoring is ordinary Java against an answer key in
  the database. The model is used only for judgement and narrative, and when it fails the
  page still renders everything else.
- **Every call is recorded.** Latency and outcome go to a Micrometer timer tagged with the
  prompt and its version; from milestone 3 the provider, model, prompt version and raw
  response are persisted with each review, which is what makes debugging a bad review
  possible after the fact.
- **Slow work happens off the request.** A local model takes tens of seconds, so model
  calls run on a small bounded executor and the page renders without waiting. No database
  transaction is ever held open across a model call.

## Accounts and limits

The catalogue and the quiz work signed out. Writing a design needs an account, because a
review costs a minute of a model's time and the free plan caps it:

| Plan | Design reviews | Quiz |
|---|---|---|
| `FREE` | 3 per calendar month | unlimited |
| `PRO` | unlimited | unlimited |

The cap is configuration (`designarena.limits.free-reviews-per-month`), checked before the
model is called, and counted from the submissions themselves rather than from a counter
that could drift. Nothing charges anybody yet; the plan flag exists so that adding
payments later is a column update rather than a policy change.

Access is declared on each view with `@AnonymousAllowed` or `@PermitAll`, not as a list of
URL patterns, so a screen that forgets to decide is denied rather than exposed.

## Signing in locally

Running with the default `local` profile creates two demo accounts on first start, so
there is something to sign in with on a fresh database:

| Account | Email | Password | Can do |
|---|---|---|---|
| Admin | `admin@designarena.local` | `admin-local-demo` | everything below, plus the admin area |
| Learner | `learner@designarena.local` | `learner-local-demo` | quiz, design reviews, history — free plan |

Both start on the **free plan**, so the three-reviews-a-month limit is visible without
extra setup. Signing up through `/signup` works too; the demo accounts just save a step.

> **These accounts exist only under the `local` profile.** They are created by an
> application runner annotated `@Profile("local")`, not by a Flyway migration — a
> migration would have created an administrator with a published password in every
> environment it ever ran in, production included. The `cloud` profile never constructs
> that bean, so there is nothing to disable before deploying.

In a real environment, promote an existing account with one line of SQL. There is
deliberately no self-service route to the role that can edit every rubric:

```sql
UPDATE app_user SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Sign out and back in afterwards: roles are read when the session is created.

## What an administrator can do

Everything below is at `/admin`, guarded by `@RolesAllowed("ADMIN")`. A signed-in person
without the role is refused by Vaadin before the view is constructed.

**Problems** (`/admin`)
- See every problem, published or draft, with its slug and state
- Create a problem from a slug — it starts **unpublished**, so half-written content is
  never visible to somebody practising

**Problem content** (`/admin/problem/{slug}`)
- Title, difficulty, catalogue blurb, and the Markdown statement
- Functional and non-functional requirements, and scale hints — ordered lists edited one
  line per item
- Publish or unpublish. Unpublishing removes a problem from the catalogue without
  deleting it, so existing history and links keep working
- The slug is fixed after creation, because URLs and stored submissions depend on it

**Rubric** (same screen)
- Add, rename, reorder and remove dimensions, each with a description, a weight and its
  must-have points
- Dimension names are sent to the model verbatim and appear on the review page
- Editing a rubric never rewrites reviews already produced: each evaluation stored its
  result with the dimension names the model was given at the time

**Questions** (`/admin/problem/{slug}/questions`)
- Add and edit questions: text, single or multi choice, and the scoring category
- Edit each option's text, whether it is correct, and its explanation
- Each question saves on its own, so a mistake in one cannot discard edits to another
- Edits are validated against the scorer's own rules and refused in plain words: a
  question needs text, at least two options, an explanation on every option including the
  wrong ones, at least one correct option, exactly one for single-choice, and at least two
  for multi-choice

**AI status** (`/admin/ai`)
- The provider and model actually answering, read from the wired beans rather than from
  configuration, plus whether model calls are enabled and how many retries are allowed
- Every prompt and its version
- Model call latency and outcome, and counters for quizzes submitted, reviews completed,
  reviews failed, and reviews blocked by the plan limit
- The same provider and model detail appears in `/actuator/info`

## How the project is organised

```
in.codefarm.designarena
├── problem/   # the catalogue: problems, requirements, rubrics
├── quiz/      # questions, options, attempts, scoring
├── review/    # submissions, evaluations, the AI review flow
├── ai/        # the one place that talks to a model
├── user/      # accounts and plans
├── ui/        # Vaadin views and shared components
└── config/
```

Packages are organised by feature, not by layer. Entities stay inside their feature and
records cross the boundaries, so no part of the app depends on another's database shape.

Written code and generated code are kept apart:

| Path | Contents |
|---|---|
| `src/main/java`, `src/main/resources` | written by hand |
| `src/main/frontend/styles` | written by hand — the design tokens |
| `src/main/frontend/generated`, `build/`, `target/`, root npm files | generated every build, git-ignored, safe to delete |

See `src/main/frontend/README.md` for why two of those cannot be moved out of the source
tree, and `./gradlew clean` for removing all of them.

- **Schema**: owned by Flyway (`src/main/resources/db/migration`). Hibernate runs with
  `ddl-auto: validate`, so a mapping that drifts from the schema fails at startup.
- **UI**: built to the approved mockups in `docs/ui/`. All styling comes from design
  tokens defined once in `src/main/frontend/styles/design-arena.css`.
- **Engineering standards**, including the build's quality gates:
  `.claude/skills/java-enterprise/SKILL.md`.
- **Build brief and milestones**: `docs/0-design-arena.md`.
- **What each milestone taught**: `docs/learnings/`.

## Quality gates

`./gradlew build` fails, rather than warns, on any of these:

| Gate | Tool |
|---|---|
| Formatting | Spotless (Palantir Java Format) |
| Static analysis | Checkstyle, zero tolerated warnings |
| Compiler warnings | `-Xlint:all -Werror` |
| Tests | JUnit 5, Testcontainers for anything touching Postgres |
| Coverage | JaCoCo, 70% instruction floor |
