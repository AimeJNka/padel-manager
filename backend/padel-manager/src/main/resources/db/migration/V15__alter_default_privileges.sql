-- V15: future-proof least-privilege — any future TABLE/SEQUENCE auto-granted to app_user.
-- Without this, every new CREATE TABLE in V16+ would need a follow-up GRANT migration
-- (the pattern that produced V6 and V8).

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO app_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT ON TABLES TO app_readonly;
