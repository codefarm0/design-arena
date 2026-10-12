-- Design Arena core schema.
--
-- Flyway owns the schema: this file is immutable once it has run anywhere. Anything that
-- needs changing arrives as a new migration.
--
-- Two modelling notes worth understanding:
--  * Requirements, scale hints and rubric must-haves are rendered as ordered lists in the
--    UI, so they are child rows with an explicit position rather than text blobs. Ordering
--    in SQL is never implicit.
--  * A quiz answer can hold several selected options (multi-choice questions), so the
--    selection is its own table instead of a delimited column.

---------------------------------------------------------------------------
-- Users
---------------------------------------------------------------------------

CREATE TABLE app_user (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email        VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    plan         VARCHAR(16)  NOT NULL DEFAULT 'FREE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT app_user_plan_check CHECK (plan IN ('FREE', 'PRO'))
);

---------------------------------------------------------------------------
-- Problem catalogue
---------------------------------------------------------------------------

CREATE TABLE problem (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug         VARCHAR(80)  NOT NULL UNIQUE,
    title        VARCHAR(160) NOT NULL,
    difficulty   VARCHAR(16)  NOT NULL,
    blurb        VARCHAR(400) NOT NULL,
    statement_md TEXT         NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    position     INTEGER      NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT problem_difficulty_check CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE INDEX idx_problem_active_position ON problem (active, position);

CREATE TABLE problem_requirement (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    problem_id BIGINT       NOT NULL REFERENCES problem (id) ON DELETE CASCADE,
    kind       VARCHAR(16)  NOT NULL,
    position   INTEGER      NOT NULL,
    text       VARCHAR(400) NOT NULL,
    CONSTRAINT problem_requirement_kind_check CHECK (kind IN ('FUNCTIONAL', 'NON_FUNCTIONAL')),
    CONSTRAINT problem_requirement_unique_position UNIQUE (problem_id, kind, position)
);

CREATE TABLE problem_scale_hint (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    problem_id BIGINT       NOT NULL REFERENCES problem (id) ON DELETE CASCADE,
    position   INTEGER      NOT NULL,
    text       VARCHAR(200) NOT NULL,
    CONSTRAINT problem_scale_hint_unique_position UNIQUE (problem_id, position)
);

---------------------------------------------------------------------------
-- Rubric: how a free-text design is judged
---------------------------------------------------------------------------

CREATE TABLE rubric (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    problem_id BIGINT      NOT NULL UNIQUE REFERENCES problem (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rubric_dimension (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rubric_id   BIGINT       NOT NULL REFERENCES rubric (id) ON DELETE CASCADE,
    position    INTEGER      NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(600) NOT NULL,
    -- Weights are relative, not percentages: the score is a weighted mean, so weights
    -- can be tuned per problem without having to make them sum to anything.
    weight      INTEGER      NOT NULL DEFAULT 1,
    CONSTRAINT rubric_dimension_weight_check CHECK (weight > 0),
    CONSTRAINT rubric_dimension_unique_position UNIQUE (rubric_id, position)
);

CREATE TABLE rubric_must_have (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rubric_dimension_id BIGINT       NOT NULL REFERENCES rubric_dimension (id) ON DELETE CASCADE,
    position            INTEGER      NOT NULL,
    text                VARCHAR(400) NOT NULL,
    CONSTRAINT rubric_must_have_unique_position UNIQUE (rubric_dimension_id, position)
);

---------------------------------------------------------------------------
-- Quiz mode
---------------------------------------------------------------------------

CREATE TABLE question (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    problem_id BIGINT      NOT NULL REFERENCES problem (id) ON DELETE CASCADE,
    position   INTEGER     NOT NULL,
    text       VARCHAR(600) NOT NULL,
    type       VARCHAR(16) NOT NULL,
    category   VARCHAR(60) NOT NULL,
    CONSTRAINT question_type_check CHECK (type IN ('SINGLE', 'MULTI')),
    CONSTRAINT question_unique_position UNIQUE (problem_id, position)
);

CREATE TABLE question_option (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id BIGINT       NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    position    INTEGER      NOT NULL,
    text        VARCHAR(400) NOT NULL,
    is_correct  BOOLEAN      NOT NULL,
    -- Every option carries an explanation, so feedback on a wrong answer can say why
    -- that option is wrong rather than only naming the right one.
    explanation VARCHAR(800) NOT NULL,
    CONSTRAINT question_option_unique_position UNIQUE (question_id, position)
);

CREATE TABLE quiz_attempt (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      REFERENCES app_user (id) ON DELETE SET NULL,
    problem_id   BIGINT      NOT NULL REFERENCES problem (id) ON DELETE CASCADE,
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ,
    score        INTEGER,
    max_score    INTEGER,
    -- The AI narrative. Nullable on purpose: the results page must render without it.
    summary_text TEXT,
    CONSTRAINT quiz_attempt_score_check CHECK (score IS NULL OR score >= 0)
);

CREATE INDEX idx_quiz_attempt_user ON quiz_attempt (user_id, started_at DESC);

CREATE TABLE quiz_answer (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    attempt_id  BIGINT NOT NULL REFERENCES quiz_attempt (id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    CONSTRAINT quiz_answer_unique_question UNIQUE (attempt_id, question_id)
);

CREATE TABLE quiz_answer_option (
    quiz_answer_id     BIGINT NOT NULL REFERENCES quiz_answer (id) ON DELETE CASCADE,
    question_option_id BIGINT NOT NULL REFERENCES question_option (id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_answer_id, question_option_id)
);

---------------------------------------------------------------------------
-- Design review mode
---------------------------------------------------------------------------

CREATE TABLE design_submission (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      REFERENCES app_user (id) ON DELETE SET NULL,
    problem_id      BIGINT      NOT NULL REFERENCES problem (id) ON DELETE CASCADE,
    content_md      TEXT        NOT NULL,
    diagram_mermaid TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_design_submission_user ON design_submission (user_id, created_at DESC);

CREATE TABLE evaluation (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    submission_id     BIGINT      NOT NULL REFERENCES design_submission (id) ON DELETE CASCADE,
    status            VARCHAR(16) NOT NULL,
    -- The parsed result as returned by the model, kept as JSON so a prompt or schema
    -- change does not invalidate evaluations that were already produced.
    result_json       JSONB,
    -- The raw response is kept even on success: it is the only way to debug a bad
    -- review after the fact, and the material for the eval set.
    raw_response      TEXT,
    provider          VARCHAR(40),
    model             VARCHAR(120),
    prompt_version    VARCHAR(20),
    latency_ms        BIGINT,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    failure_reason    VARCHAR(400),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,
    CONSTRAINT evaluation_status_check CHECK (status IN ('PENDING', 'DONE', 'FAILED'))
);

CREATE INDEX idx_evaluation_submission ON evaluation (submission_id, created_at DESC);
