# UI Contract

Extracted from the approved mockups in `docs/ui/ui/`. Those files are the visual
authority; this file is the machine-readable summary of them. If the two disagree,
the mockup wins — re-read it and correct this file.

The mockups are design-tool exports (`<x-dc>`, `<sc-for>`, `<sc-if>`, inline styles,
a missing `support.js`). **Never copy their markup or inline styles.** Read them for
layout, tokens, copy and states, then implement in Vaadin Flow against the tokens below.

## Tokens

Define these once as CSS custom properties in the Vaadin theme
(`src/main/frontend/themes/design-arena/styles.css`). No hex literal anywhere else —
not in Java, not in a view, not in a second stylesheet.

### Colour

| Token | Value | Use |
|---|---|---|
| `--da-bg` | `#F5F6F8` | page background; also inset explanation/code blocks on white |
| `--da-surface` | `#FFFFFF` | cards, header, inputs |
| `--da-surface-muted` | `#FAFBFC` | diagram preview pane |
| `--da-border` | `#DDE1E7` | default card/input border, progress track on white |
| `--da-border-strong` | `#C9CED6` | secondary button border; subtext on dark banner |
| `--da-text` | `#14161B` | headings, primary text; dark banner background |
| `--da-text-body` | `#2A2E35` | body copy inside cards |
| `--da-text-muted` | `#555B66` | labels, meta, captions |
| `--da-text-chip` | `#3A3F48` | chip text; neutral status dot |
| `--da-brand` | `#0E6B5C` | primary buttons, links, selected state, progress fill |
| `--da-brand-dark` | `#0A5246` | link hover; text on brand tint |
| `--da-brand-tint` | `#E3F1EE` | selected option, AI badge, info banner, correct icon |
| `--da-brand-tint-text` | `#2F3A37` | body text on brand tint |
| `--da-chip-bg` | `#EEF0F3` | category / question-type chips |
| `--da-track` | `#E8EBEF` | score bars on white |
| `--da-danger` | `#B42318` | incorrect mark, failed-state button |
| `--da-danger-bg` | `#FDECEA` | failed-state banner, incorrect icon |
| `--da-danger-border` | `#F3C4BE` | failed-state banner border |
| `--da-danger-text` | `#8A1C12` | failed-state heading |
| `--da-danger-text-soft` | `#5C1A13` | failed-state body |
| `--da-warn` | `#B45309` | low score bar; "what's missing" dot |
| `--da-neutral` | `#6B7280` | mid score bar |

Score-bar colour is a rule, not a choice: `>= 7` brand, `5–6` neutral, `< 5` warn.

### Type

- **Sans**: IBM Plex Sans (400/500/600/700), fallback `system-ui, sans-serif`. Base `16px / 1.5`.
- **Mono**: IBM Plex Mono (400/500) — chips, scores, counters, metadata, question numbers,
  scale hints, the Markdown and Mermaid textareas, and the provider/model footer. Mono signals
  "machine-generated or code"; never use it for prose.
- Self-host both fonts in the theme. No Google Fonts request at runtime (the mockup's CDN
  `<link>` is an export artifact).

| Role | Size | Weight | Tracking |
|---|---|---|---|
| Hero h1 (catalogue) | 44px / 1.1 | 700 | `-0.02em` |
| Page h1 (results, review) | 32px | 700 | `-0.02em` |
| Question h1 | 24px / 1.35 | 600 | — |
| Section h2 | 20–22px | 600 | — |
| Card h3 | 20px | 600 | — |
| Lead paragraph | 18px | 400 | — |
| Body | 15–16px | 400 | — |
| Meta / caption | 13–14px | 400 | — |
| Chip | 12px mono | 500 | `0.04em` on eyebrow text |
| Big score | 64px / 1 | 700 | `-0.03em` |

### Shape and space

- Radius: `8px` buttons and inputs, `10px` selectable options and inner panels,
  `12px` cards and banners, `999px` pills, chips and bars.
- Bar height `8px`.
- Container max-width per screen: catalogue and review `1120px`, editor `1240px`,
  results `960px`, quiz `760px`. Gutter `24px`.
- Main padding: `40px` top (`56px` on the catalogue), `64px` bottom.
- Card padding `24px`; the quiz question card `32px`.
- Gaps: `20–24px` between cards and grid cells, `12–16px` inside a card, `8px` in a tight stack.
- Card grids are `repeat(auto-fit, minmax(300px, 1fr))` — never a fixed column count.
- The editor is a two-pane flex: problem aside `flex: 1 1 320px`, editor column
  `flex: 999 1 560px`, so the aside collapses under the editor on narrow screens.

### Controls

- Every interactive target is **at least 44px tall**. This is a hard accessibility floor.
- Primary: brand fill, white text, 600 weight. Secondary: white fill, `--da-border-strong`
  border, 500 weight. Tertiary: text only, muted colour.
- Selected option: `2px` brand border on brand tint. Unselected: `1px` default border on
  surface, with `1px` margin so the box does not shift when selection thickens the border.

## Screens

Each mockup maps to one Vaadin view. Keep the copy — it was written deliberately.

| Mockup | View | Route |
|---|---|---|
| `Main.html` | problem catalogue | `/` |
| `Quiz.html` | one quiz question | `/quiz/{slug}` |
| `QuizResults.html` | quiz results | `/quiz/{slug}/results/{attemptId}` |
| `Editor.html` | design editor | `/design/{slug}` |
| `Review.html` | design review result | `/review/{submissionId}` |

Shared header on every screen: logo mark plus "Design Arena" wordmark linking home, on
white with a bottom border. Its right side is contextual — full nav with "Sign in" on the
catalogue, "Exit quiz" during a quiz, "Back to problems" on result screens, draft status
plus "Submit for review" in the editor.

**Catalogue** — eyebrow + hero + lead, then a problem-card grid (difficulty chip, question
count, title, blurb, "Take quiz" primary and "Write a design" secondary), then the
codefarm.in learning banner on brand tint.

**Quiz** — problem label and `Question N of M` in mono above a progress bar, then one
question card: category and type chips, question, options as a radio or checkbox fieldset
with a visually hidden legend. Footer: "← Previous" left; "Skip" and "Next →" right.

**Quiz results** — overall score tile (64px numeral, `/ 10`, and the line
"Scored by the answer key, not the AI."), per-category bars, the AI summary card with an
`AI SUMMARY` chip and the note that score and explanations still render without it, then
question-by-question cards with a correct/incorrect icon, the user's answer, the correct
answer when wrong, and the explanation on an inset background. Collapsed to 3 with a
"Show all" button. Closes with the dark CTA banner to the design editor.

**Editor** — problem aside (difficulty eyebrow, title, statement, functional and
non-functional requirement lists, scale hints in mono on an inset panel, codefarm.in link)
beside the editor column: labelled Markdown textarea with a live `N / 15,000` counter, then
the optional Mermaid textarea with a live preview beside it, then the note that review takes
a minute or two and results land in History.

**Review** — three mutually exclusive states driven by evaluation status:

- `PENDING` → centred spinner, "Reviewing your design…", the dimension count, and the
  "you can leave" note. Container is `role="status"`.
- `FAILED` → danger banner, "The review didn't finish", reassurance that the design is
  saved, and a "Retry review" button. Container is `role="alert"`.
- `DONE` → overall score tile with a one-line verdict, the seven rubric dimensions as
  name / bar / score rows, four finding cards each with a coloured dot (well = brand,
  wrong or risky = danger, missing = warn, follow-up questions = chip grey), the original
  submission in a collapsed `<details>`, and a mono footer naming model, provider and
  prompt version beside "Revise and resubmit" and "Try another problem".

## Accessibility

The mockups already encode these. Preserve them in the Vaadin implementation:

- Progress bar: `role="progressbar"` with `aria-valuenow/min/max` and a label.
- Option groups: a real fieldset with a visually hidden legend ("Choose one answer").
- Async states: `role="status"` while reviewing, `role="alert"` on failure.
- Icons: `aria-hidden="true"` when decorative, `role="img"` with an `aria-label`
  ("Correct" / "Incorrect") when they carry meaning.
- Every input has an associated label. Correct and incorrect are never signalled by
  colour alone — the icon shape and the text carry it too.
- Keyboard reachable in visual order, with a visible focus ring (the mockups use
  `outline-offset: -2px` on textareas).
