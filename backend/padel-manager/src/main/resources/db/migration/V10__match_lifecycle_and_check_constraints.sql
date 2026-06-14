-- V10__match_lifecycle_and_check_constraints.sql
-- Purpose: enforce valid statut/type values on match_padel and participation,
--          add the DEMARRE lifecycle status, and create scheduler-oriented indexes.

-- 1) Enforce valid statut values for match_padel.
--    DEMARRE is the idempotency marker written by Job 3 (solde organisateur)
--    to prevent double-charging on repeated scheduler runs.
ALTER TABLE match_padel
    ADD CONSTRAINT chk_match_padel_statut
    CHECK (statut IN ('EN_ATTENTE', 'DEMARRE', 'ANNULE'));

-- 2) Enforce valid type values for match_padel.
ALTER TABLE match_padel
    ADD CONSTRAINT chk_match_padel_type
    CHECK (type_match IN ('PRIVE', 'PUBLIC'));

-- 3) Enforce valid statut values for participation.
ALTER TABLE participation
    ADD CONSTRAINT chk_participation_statut
    CHECK (statut IN ('EN_ATTENTE', 'CONFIRME', 'ANNULEE'));

-- 4) Scheduler-oriented indexes.
--    Job 1 (private match bascule): filters on type_match + statut.
--    Job 2 (unpaid slot release):   filters on participation.statut.
--    Job 3 (organizer solde):       filters on match_padel.statut + disponibilite join.
CREATE INDEX IF NOT EXISTS idx_match_padel_statut
    ON match_padel(statut);

CREATE INDEX IF NOT EXISTS idx_match_padel_type_statut
    ON match_padel(type_match, statut);

CREATE INDEX IF NOT EXISTS idx_disponibilite_date_debut
    ON disponibilite(date_heure_debut);

-- Note: idx_paiement_statut, idx_penalite_matricule, idx_penalite_date_fin
-- were already created in V8. No duplicates here.
