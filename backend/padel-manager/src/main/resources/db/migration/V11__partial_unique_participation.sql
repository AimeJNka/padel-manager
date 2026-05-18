-- V11__partial_unique_participation.sql
-- Replace strict UNIQUE constraint on (id_match, matricule) with a partial
-- unique index excluding ANNULEE rows, to allow re-registration after cancellation.
-- See docs/adr/0003-partial-unique-participation.md for rationale.

ALTER TABLE participation DROP CONSTRAINT participation_id_match_matricule_key;

CREATE UNIQUE INDEX participation_match_membre_active_uidx
    ON participation (id_match, matricule)
    WHERE statut <> 'ANNULEE';
