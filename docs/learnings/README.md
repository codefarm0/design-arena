# Learning stories

One story per milestone, written to be published as it stands. Each is 1500–1700 words,
carries its own architecture and flow diagrams in Mermaid, explains the technologies the
first time they appear, and is honest about what broke.

| # | Story | Milestone | Covers |
|---|---|---|---|
| 1 | [Build the gate before you build the app](m1-skeleton.md) | M1 — Skeleton | Enforcing Gradle build, Docker Compose, Flyway schema and seed data, the problem catalogue, design tokens |
| 2 | [The model writes the paragraph, not the score](m2-quiz-mode.md) | M2 — Quiz mode | Deterministic scoring, the single AI seam, prompts as files, background generation with a fallback |
| 3 | [Asking a model for JSON, and what to do when it says no](m3-design-review.md) | M3 — Design review | Structured output, bounded retries, a FAILED state with retry, the eval set |
| 4 | [The cheapest way to lock a door is to forget to open it](m4-accounts-and-limits.md) | M4 — Users and limits | Spring Security with per-view rules, accounts, history, a monthly cap counted not tracked |
| 5 | [Three constraints, two of which I had written myself](m5-admin-and-observability.md) | M5 — Polish | Admin screens for problems, questions and rubrics; the AI status page; usage counters |

The design and scope they are all built against live in
[`../0-design-arena.md`](../0-design-arena.md).
