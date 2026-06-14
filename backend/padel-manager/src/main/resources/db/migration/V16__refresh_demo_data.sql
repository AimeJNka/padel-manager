-- ═══════════════════════════════════════════════════════════════════
-- V16 — Refresh demo business data for the jury demo (REFRESH-DEMO-DATA)
--
-- Why: V12/V14 seeded matches relative to their apply date. On a DB that
--      was migrated weeks ago, those matches are now in the past and the
--      "matchs publics" view shows obsolete, non-inscribable matches.
--      This migration wipes the demo BUSINESS data (matches, participations,
--      paiements, pénalités) and re-seeds a fresh, internally-consistent set
--      covering the full match lifecycle for a rich jury demo.
--
-- What is KEPT (structural): membres, administrateurs, sites, terrains,
--      horaires_annuels, fermetures, and the LIBRE disponibilité grid (V12).
--
-- Dates: CURRENT_DATE / NOW()-relative (project pattern, cf. V12). NOW() is
--      transaction-stable in PostgreSQL, so the same expression used to create
--      a slot and to look it up binds to the exact same row within this script.
--
-- 7 matches:
--   | # | Type   | Statut     | Slot               | Site/Terr | Org   |
--   | 1 | PUBLIC | EN_ATTENTE | J+3 09:00          | 1 / 3     | G0002 |  3/4 — G0001 peut s'inscrire en live
--   | 2 | PUBLIC | EN_ATTENTE | J+5 13:30          | 2 / 2     | S0005 |  2/4
--   | 3 | PRIVE  | EN_ATTENTE | J+4 10:45 (>24h)   | 1 / 1     | G0001 |  4/4 — G0001 doit encore payer sa part
--   | 4 | PUBLIC | DEMARRE    | en cours (NOW±)    | 2 / 5     | S0004 |  4 confirmés
--   | 5 | PRIVE  | DEMARRE    | en cours (NOW±)    | 1 / 5     | G0002 |  4 confirmés
--   | 6 | PUBLIC | EFFECTUE   | J-3 (historique)   | 1 / 6     | G0003 |  4 confirmés
--   | 7 | PRIVE  | ANNULE     | J+6 15:15          | 2 / 1     | S0001 |  annulé (vue admin)
--
-- DEMO TIMING: les 2 matchs DEMARRE sont "en cours" (~1h après reset).
--   Réinitialiser la base au plus tard 30 min avant la démo pour garder
--   ces états stables (sinon le scheduler les fera passer à EFFECTUE).
--
-- Password hash inchangé (BCrypt "password", strength 10) — aucun membre créé ici.
-- ═══════════════════════════════════════════════════════════════════


-- ─────────────────────────────────────────────────────────────────
-- Section 1 — Cleanup: remove all V12/V14 demo business data
-- FK order: paiement → participation → match_padel. penalite is independent.
-- ─────────────────────────────────────────────────────────────────
DELETE FROM paiement      WHERE id_participation IN (SELECT id_participation FROM participation);
DELETE FROM participation;
DELETE FROM match_padel;
DELETE FROM penalite;

-- Free every slot previously RESERVE by a now-deleted match
UPDATE disponibilite SET statut = 'LIBRE' WHERE statut <> 'LIBRE';


-- ─────────────────────────────────────────────────────────────────
-- Section 2 — Disponibilités for past / in-progress matches
-- V12 only generated slots for CURRENT_DATE..+13, so DEMARRE/EFFECTUE
-- need dedicated slots here. Created directly as RESERVE (occupied).
-- UNIQUE (date_heure_debut, id_terrain) — ON CONFLICT for replay safety.
-- ─────────────────────────────────────────────────────────────────

-- Match 4 (DEMARRE, in-progress): site 2, terrain 5 — started 20 min ago, ends in 70 min
INSERT INTO disponibilite (id_terrain, date_heure_debut, date_heure_fin, statut)
SELECT t.id_terrain,
       (NOW() - INTERVAL '20 minutes')::timestamp,
       (NOW() + INTERVAL '70 minutes')::timestamp,
       'RESERVE'
FROM terrain t
WHERE t.id_site = 2 AND t.numero = 5
ON CONFLICT (date_heure_debut, id_terrain) DO NOTHING;

-- Match 5 (DEMARRE, in-progress): site 1, terrain 5 — started 40 min ago, ends in 50 min
INSERT INTO disponibilite (id_terrain, date_heure_debut, date_heure_fin, statut)
SELECT t.id_terrain,
       (NOW() - INTERVAL '40 minutes')::timestamp,
       (NOW() + INTERVAL '50 minutes')::timestamp,
       'RESERVE'
FROM terrain t
WHERE t.id_site = 1 AND t.numero = 5
ON CONFLICT (date_heure_debut, id_terrain) DO NOTHING;

-- Match 6 (EFFECTUE, history): site 1, terrain 6 — 3 days ago, 90-min slot
INSERT INTO disponibilite (id_terrain, date_heure_debut, date_heure_fin, statut)
SELECT t.id_terrain,
       (NOW() - INTERVAL '3 days')::timestamp,
       (NOW() - INTERVAL '3 days' + INTERVAL '90 minutes')::timestamp,
       'RESERVE'
FROM terrain t
WHERE t.id_site = 1 AND t.numero = 6
ON CONFLICT (date_heure_debut, id_terrain) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 3 — Matchs (locator = site + terrain.numero + exact slot start)
-- Future matches reuse the existing V12 LIBRE grid slots.
-- ─────────────────────────────────────────────────────────────────

-- Match 1: PUBLIC EN_ATTENTE — site 1, terrain 3, J+3 09:00, G0002
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0002', 'PUBLIC', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '2 days'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '3 days' + 540 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 2: PUBLIC EN_ATTENTE — site 2, terrain 2, J+5 13:30, S0005
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0005', 'PUBLIC', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '1 day'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '5 days' + 810 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 3: PRIVE EN_ATTENTE — site 1, terrain 1, J+4 10:45 (>24h), G0001
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0001', 'PRIVE', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '1 day'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 645 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 4: PUBLIC DEMARRE — site 2, terrain 5, in-progress, S0004
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0004', 'PUBLIC', 'DEMARRE', 60.00, NOW() - INTERVAL '6 days'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 5
  AND d.date_heure_debut = (NOW() - INTERVAL '20 minutes')::timestamp
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 5: PRIVE DEMARRE — site 1, terrain 5, in-progress, G0002
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0002', 'PRIVE', 'DEMARRE', 60.00, NOW() - INTERVAL '5 days'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 5
  AND d.date_heure_debut = (NOW() - INTERVAL '40 minutes')::timestamp
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 6: PUBLIC EFFECTUE — site 1, terrain 6, J-3, G0003
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0003', 'PUBLIC', 'EFFECTUE', 60.00, NOW() - INTERVAL '10 days'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 6
  AND d.date_heure_debut = (NOW() - INTERVAL '3 days')::timestamp
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 7: PRIVE ANNULE — site 2, terrain 1, J+6 15:15, S0001 (dispo stays LIBRE)
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0001', 'PRIVE', 'ANNULE', 60.00, NOW() - INTERVAL '8 days'
FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '6 days' + 915 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 4 — Participations (22 rows)
-- Statut: EN_ATTENTE (not yet paid) / CONFIRME (paid) / ANNULEE (cancelled).
-- V11 partial unique index: (id_match, matricule) WHERE statut <> 'ANNULEE'.
-- ─────────────────────────────────────────────────────────────────

-- Match 1 (PUBLIC EN_ATTENTE) — G0002 org, S0003 paid, L0002 pending
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, v.matricule, v.statut, NOW() - v.ago
FROM (VALUES
        ('G0002', 'EN_ATTENTE', INTERVAL '2 days'),
        ('S0003', 'CONFIRME',   INTERVAL '40 hours'),
        ('L0002', 'EN_ATTENTE', INTERVAL '30 hours')
     ) AS v(matricule, statut, ago)
JOIN match_padel m  ON m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 1 AND t.numero = 3
          AND d.date_heure_debut = CURRENT_DATE + INTERVAL '3 days' + 540 * INTERVAL '1 minute')
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 2 (PUBLIC EN_ATTENTE) — S0005 org, S0006 paid
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, v.matricule, v.statut, NOW() - v.ago
FROM (VALUES
        ('S0005', 'EN_ATTENTE', INTERVAL '1 day'),
        ('S0006', 'CONFIRME',   INTERVAL '18 hours')
     ) AS v(matricule, statut, ago)
JOIN match_padel m ON m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 2 AND t.numero = 2
          AND d.date_heure_debut = CURRENT_DATE + INTERVAL '5 days' + 810 * INTERVAL '1 minute')
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 3 (PRIVE EN_ATTENTE) — G0001 org (still to pay), 3 others paid
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, v.matricule, v.statut, NOW() - v.ago
FROM (VALUES
        ('G0001', 'EN_ATTENTE', INTERVAL '1 day'),
        ('G0003', 'CONFIRME',   INTERVAL '20 hours'),
        ('S0002', 'CONFIRME',   INTERVAL '19 hours'),
        ('L0003', 'CONFIRME',   INTERVAL '18 hours')
     ) AS v(matricule, statut, ago)
JOIN match_padel m ON m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 1 AND t.numero = 1
          AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 645 * INTERVAL '1 minute')
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 4 (PUBLIC DEMARRE) — 4 confirmed
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, v.matricule, 'CONFIRME', NOW() - v.ago
FROM (VALUES
        ('S0004', INTERVAL '5 days'),
        ('S0005', INTERVAL '4 days'),
        ('S0006', INTERVAL '4 days'),
        ('G0003', INTERVAL '3 days')
     ) AS v(matricule, ago)
JOIN match_padel m ON m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 2 AND t.numero = 5
          AND d.date_heure_debut = (NOW() - INTERVAL '20 minutes')::timestamp)
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 5 (PRIVE DEMARRE) — 4 confirmed
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, v.matricule, 'CONFIRME', NOW() - v.ago
FROM (VALUES
        ('G0002', INTERVAL '5 days'),
        ('S0002', INTERVAL '4 days'),
        ('S0003', INTERVAL '4 days'),
        ('L0002', INTERVAL '3 days')
     ) AS v(matricule, ago)
JOIN match_padel m ON m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 1 AND t.numero = 5
          AND d.date_heure_debut = (NOW() - INTERVAL '40 minutes')::timestamp)
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 6 (PUBLIC EFFECTUE) — 4 confirmed (history)
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, v.matricule, 'CONFIRME', NOW() - v.ago
FROM (VALUES
        ('G0003', INTERVAL '8 days'),
        ('S0004', INTERVAL '7 days'),
        ('S0005', INTERVAL '7 days'),
        ('S0006', INTERVAL '6 days')
     ) AS v(matricule, ago)
JOIN match_padel m ON m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 1 AND t.numero = 6
          AND d.date_heure_debut = (NOW() - INTERVAL '3 days')::timestamp)
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 7 (PRIVE ANNULE) — S0001 org, ANNULEE (outside partial index, no ON CONFLICT)
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0001', 'ANNULEE', NOW() - INTERVAL '8 days'
FROM match_padel m
WHERE m.id_dispo IN (
        SELECT d.id_dispo FROM disponibilite d JOIN terrain t ON t.id_terrain = d.id_terrain
        WHERE t.id_site = 2 AND t.numero = 1
          AND d.date_heure_debut = CURRENT_DATE + INTERVAL '6 days' + 915 * INTERVAL '1 minute');


-- ─────────────────────────────────────────────────────────────────
-- Section 5 — Paiements (1 per participation, 22 rows)
-- CONFIRME → PAYE (date set) · EN_ATTENTE → EN_ATTENTE (date NULL)
-- ANNULEE  → ANNULE (date NULL). montant 15.00, solde_inclus 0.00.
-- UNIQUE (id_participation).
-- ─────────────────────────────────────────────────────────────────

-- Paid participations (CONFIRME) → PAYE, date_paiement in the past
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, p.date_inscription + INTERVAL '2 hours', 'PAYE'
FROM participation p
WHERE p.statut = 'CONFIRME'
ON CONFLICT (id_participation) DO NOTHING;

-- Pending participations (EN_ATTENTE) → EN_ATTENTE, date_paiement NULL
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
WHERE p.statut = 'EN_ATTENTE'
ON CONFLICT (id_participation) DO NOTHING;

-- Cancelled participations (ANNULEE) → ANNULE, date_paiement NULL
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'ANNULE'
FROM participation p
WHERE p.statut = 'ANNULEE'
ON CONFLICT (id_participation) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 6 — Mark occupied slots RESERVE (mirrors creerMatch service).
-- ANNULE matches leave their slot LIBRE (mirrors app annulation behaviour).
-- DEMARRE/EFFECTUE slots were already RESERVE at creation (Section 2); idempotent.
-- ─────────────────────────────────────────────────────────────────
UPDATE disponibilite d
SET statut = 'RESERVE'
FROM match_padel m
WHERE m.id_dispo = d.id_dispo
  AND m.statut <> 'ANNULE';


-- ─────────────────────────────────────────────────────────────────
-- Section 7 — Pénalités (2 rows, both on peripheral V14 members)
-- 0 penalty on G0001 and on any organiser of the 7 new matches.
-- ─────────────────────────────────────────────────────────────────

-- Active penalty (peripheral member S0101 — site 2, not in any match)
INSERT INTO penalite (matricule, date_debut, date_fin, motif)
VALUES ('S0101', NOW() - INTERVAL '5 days', NOW() + INTERVAL '5 days', 'Absence non justifiée');

-- Historical (expired) penalty (peripheral member L0100)
INSERT INTO penalite (matricule, date_debut, date_fin, motif)
VALUES ('L0100', NOW() - INTERVAL '30 days', NOW() - INTERVAL '10 days', 'Désistement tardif');


-- ═══════════════════════════════════════════════════════════════════
-- Section 8 — Verification (copy-paste after Flyway applies V16)
--
--   SELECT statut, COUNT(*) FROM match_padel  GROUP BY statut ORDER BY statut;
--     EN_ATTENTE → 3 | DEMARRE → 2 | EFFECTUE → 1 | ANNULE → 1   (total 7)
--   SELECT statut, COUNT(*) FROM participation GROUP BY statut ORDER BY statut;
--     EN_ATTENTE → 4 | CONFIRME → 17 | ANNULEE → 1               (total 22)
--   SELECT statut, COUNT(*) FROM paiement      GROUP BY statut ORDER BY statut;
--     EN_ATTENTE → 4 | PAYE → 17 | ANNULE → 1                    (total 22)
--   SELECT COUNT(*) FROM penalite;                               -- 2
--
-- Demo sanity:
--   - G0001 : organiser of match 3 (PRIVE), own paiement still EN_ATTENTE.
--   - Matchs publics : matches 1 & 2 (future, EN_ATTENTE) open to inscription.
--   - 0 active penalty on G0001 (login + create flow unobstructed).
--   - Reset DB <= 30 min before demo to keep matches 4 & 5 in DEMARRE.
-- ═══════════════════════════════════════════════════════════════════
