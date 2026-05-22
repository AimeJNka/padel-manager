-- V13__add_effectue_status.sql
-- Purpose: extend match_padel.statut constraint to allow EFFECTUE,
-- the terminal status written by Job 4 when a slot's end time has passed.
-- DEMARRE remains unchanged as Job 3's idempotency marker (debt at slot start).
-- Rationale: FEATURE-EFFECTUE Sprint B1.

-- 1) Extend the statut constraint to include EFFECTUE.
--    Drop by its exact name created in V10__match_lifecycle_and_check_constraints.sql.
ALTER TABLE match_padel DROP CONSTRAINT chk_match_padel_statut;
ALTER TABLE match_padel ADD CONSTRAINT chk_match_padel_statut
    CHECK (statut IN ('EN_ATTENTE', 'DEMARRE', 'ANNULE', 'EFFECTUE'));

-- 2) Index on disponibilite.date_heure_fin to support Job 4's time filter.
--    V10 created idx_disponibilite_date_debut for Job 3; this mirrors it for fin.
CREATE INDEX IF NOT EXISTS idx_disponibilite_date_fin
    ON disponibilite(date_heure_fin);
