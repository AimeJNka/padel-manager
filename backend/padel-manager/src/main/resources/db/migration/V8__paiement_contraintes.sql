-- V8__paiement_contraintes.sql
-- Purpose: enforce valid statut values on paiement, add lookup indexes,
--          and grant least-privilege access to app_user (forward-looking,
--          consistent with V4__grant_permissions.sql / V6__grant_refresh_token.sql).

-- 1) Enforce valid statut values for paiement
ALTER TABLE paiement
    ADD CONSTRAINT chk_paiement_statut
    CHECK (statut IN ('EN_ATTENTE', 'PAYE', 'ANNULE', 'REMBOURSE'));

-- 2) Performance indexes
CREATE INDEX IF NOT EXISTS idx_paiement_statut    ON paiement(statut);
CREATE INDEX IF NOT EXISTS idx_penalite_matricule ON penalite(matricule);
CREATE INDEX IF NOT EXISTS idx_penalite_date_fin  ON penalite(date_fin);

-- 3) Grants for app_user (forward-looking, consistent with V4/V6)
GRANT SELECT, INSERT, UPDATE ON TABLE paiement TO app_user;
GRANT SELECT, INSERT, UPDATE ON TABLE penalite TO app_user;
GRANT USAGE, SELECT ON SEQUENCE paiement_id_paiement_seq TO app_user;
GRANT USAGE, SELECT ON SEQUENCE penalite_id_penalite_seq TO app_user;
