-- Roles arrive with the first screen that needs protecting.
--
-- M4 deliberately gave everybody the same single authority, because what a person could
-- do depended on their plan rather than on a role. M5 adds an admin area for editing
-- problems, questions and rubrics, which is the first thing that genuinely needs one.
--
-- Plan and role are separate on purpose: a plan is what somebody paid for, a role is what
-- they are trusted with. Conflating them would make a paid account an administrator.

ALTER TABLE app_user
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT app_user_role_check CHECK (role IN ('USER', 'ADMIN'));
