-- ═══════════════════════════════════════════════
-- V6 — Permissions sur refresh_token pour app_user et app_readonly
-- (V4 ne couvre que les tables existantes à son exécution)
-- ═══════════════════════════════════════════════

GRANT SELECT, INSERT, UPDATE, DELETE ON refresh_token TO app_user;
GRANT SELECT ON refresh_token TO app_readonly;
GRANT USAGE, SELECT ON SEQUENCE refresh_token_id_refresh_token_seq TO app_user;
