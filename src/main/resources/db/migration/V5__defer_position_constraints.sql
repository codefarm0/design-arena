-- Make the ordering constraints deferrable.
--
-- The admin screens replace an ordered list wholesale: clear the old rows, add the new
-- ones. Hibernate flushes those inserts before the deletes, so for an instant the table
-- holds two rows at position 1 and a unique constraint fires - even though the state at
-- commit is perfectly valid.
--
-- The honest reading is that the invariant was always "positions are unique when the
-- transaction commits", not "after every statement". Postgres can enforce exactly that,
-- and a deferred constraint is still a constraint: a genuine duplicate still fails, just
-- at COMMIT instead of at INSERT.
--
-- Postgres cannot alter a constraint into being deferrable, so each is dropped and
-- recreated.

ALTER TABLE problem_requirement
    DROP CONSTRAINT problem_requirement_unique_position,
    ADD CONSTRAINT problem_requirement_unique_position
        UNIQUE (problem_id, kind, position) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE problem_scale_hint
    DROP CONSTRAINT problem_scale_hint_unique_position,
    ADD CONSTRAINT problem_scale_hint_unique_position
        UNIQUE (problem_id, position) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE rubric_dimension
    DROP CONSTRAINT rubric_dimension_unique_position,
    ADD CONSTRAINT rubric_dimension_unique_position
        UNIQUE (rubric_id, position) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE rubric_must_have
    DROP CONSTRAINT rubric_must_have_unique_position,
    ADD CONSTRAINT rubric_must_have_unique_position
        UNIQUE (rubric_dimension_id, position) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE question
    DROP CONSTRAINT question_unique_position,
    ADD CONSTRAINT question_unique_position
        UNIQUE (problem_id, position) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE question_option
    DROP CONSTRAINT question_option_unique_position,
    ADD CONSTRAINT question_option_unique_position
        UNIQUE (question_id, position) DEFERRABLE INITIALLY DEFERRED;
