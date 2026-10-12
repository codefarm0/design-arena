-- Accounts become real in milestone 4.
--
-- V1 created app_user with an email, a display name and a plan, because those are what
-- the rest of the application needs to know about a person. It had no credentials,
-- because until now nobody could log in. This adds them.

ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100) NOT NULL;

-- Email is the login name, and it is matched case-insensitively: somebody who registers
-- as Alice@example.com must be able to sign in as alice@example.com. Addresses are
-- stored lower-cased, and this index makes the lookup on every sign-in a single probe.
CREATE UNIQUE INDEX idx_app_user_email_lower ON app_user (lower(email));
