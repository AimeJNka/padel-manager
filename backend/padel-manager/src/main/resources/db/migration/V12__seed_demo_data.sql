-- ═══════════════════════════════════════════════════════════════════
-- V12 — Demo seed data for Phase 2 frontend testing (F-SEED)
--
-- Purpose: populate a realistic dataset for full end-to-end demo:
--   match creation, inscription, payment, penalty display, history.
--
-- Sections:
--   1. Terrains     — bring each site to 6 terrains (V3 had 5 total)
--   2. Personnes    — 9 new persons (distinct emails)
--   3. Membres      — 9 new members: 2G + 5S + 2L
--   4. Disponibilités — 14 rolling days, site-specific slot schedules
--   5. Matchs       — 7 matches in varied states + RESERVE dispo update
--   6. Participations — 11 rows replicating creerMatch side effects
--   7. Paiements    — 11 rows (1 per participation)
--   8. Pénalité     — 1 active penalty on S0002
--   9. Verification — query block (comments only)
--
-- All INSERTs use ON CONFLICT DO NOTHING for idempotence.
-- Dates computed relative to CURRENT_DATE so slots stay relevant on replay.
-- Password hash: BCrypt("password", strength=10) — same as V3.
-- ═══════════════════════════════════════════════════════════════════


-- ─────────────────────────────────────────────────────────────────
-- Section 1: Terrains — bring each site to 6
-- V3 seeded: site 1 → num 1,2,3  /  site 2 → num 1,2
-- UNIQUE constraint: (numero, id_site)
-- ─────────────────────────────────────────────────────────────────
INSERT INTO terrain (id_site, numero, statut) VALUES
    (1, 4, 'ACTIF'),
    (1, 5, 'ACTIF'),
    (1, 6, 'ACTIF'),
    (2, 3, 'ACTIF'),
    (2, 4, 'ACTIF'),
    (2, 5, 'ACTIF'),
    (2, 6, 'ACTIF')
ON CONFLICT (numero, id_site) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 2: Personnes — 9 new (emails distinct from V3)
-- V3 emails: global@test.be, site@test.be, libre@test.be
-- UNIQUE constraint: email
-- ─────────────────────────────────────────────────────────────────
INSERT INTO personne (email, nom, prenom, telephone) VALUES
    ('pierre.lecomte@demo.padel',   'Lecomte',  'Pierre',    '+32 478 10 20 30'),
    ('isabelle.renard@demo.padel',  'Renard',   'Isabelle',  '+32 478 11 21 31'),
    ('thomas.fontaine@demo.padel',  'Fontaine', 'Thomas',    '+32 478 12 22 32'),
    ('julie.lambert@demo.padel',    'Lambert',  'Julie',     '+32 478 13 23 33'),
    ('nicolas.pirard@demo.padel',   'Pirard',   'Nicolas',   '+32 478 14 24 34'),
    ('emma.delcourt@demo.padel',    'Delcourt', 'Emma',      '+32 478 15 25 35'),
    ('maxime.leclercq@demo.padel',  'Leclercq', 'Maxime',    '+32 478 16 26 36'),
    ('christine.baert@demo.padel',  'Baert',    'Christine', '+32 478 17 27 37'),
    ('antoine.demoulin@demo.padel', 'Demoulin', 'Antoine',   '+32 478 18 28 38')
ON CONFLICT (email) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 3: Membres — 9 new
--   G0002, G0003           → type 1 (GLOBAL),  id_site NULL
--   S0002, S0003           → type 2 (SITE),     id_site 1
--   S0004, S0005, S0006    → type 2 (SITE),     id_site 2
--   L0002, L0003           → type 3 (LIBRE),    id_site NULL
--
-- id_personne resolved by subquery on email (SERIAL not predictable).
-- BCrypt hash = "password" strength 10 — same hash as V3 members.
-- UNIQUE constraint: matricule (PK)
-- ─────────────────────────────────────────────────────────────────
INSERT INTO membre (matricule, id_personne, id_type, id_site, mot_de_passe, date_inscription, solde_du) VALUES
    ('G0002',
     (SELECT id_personne FROM personne WHERE email = 'pierre.lecomte@demo.padel'),
     1, NULL,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '60 days', 0.00),
    ('G0003',
     (SELECT id_personne FROM personne WHERE email = 'isabelle.renard@demo.padel'),
     1, NULL,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '45 days', 0.00),
    ('S0002',
     (SELECT id_personne FROM personne WHERE email = 'thomas.fontaine@demo.padel'),
     2, 1,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '30 days', 0.00),
    ('S0003',
     (SELECT id_personne FROM personne WHERE email = 'julie.lambert@demo.padel'),
     2, 1,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '22 days', 0.00),
    ('S0004',
     (SELECT id_personne FROM personne WHERE email = 'nicolas.pirard@demo.padel'),
     2, 2,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '40 days', 0.00),
    ('S0005',
     (SELECT id_personne FROM personne WHERE email = 'emma.delcourt@demo.padel'),
     2, 2,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '18 days', 0.00),
    ('S0006',
     (SELECT id_personne FROM personne WHERE email = 'maxime.leclercq@demo.padel'),
     2, 2,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '10 days', 0.00),
    ('L0002',
     (SELECT id_personne FROM personne WHERE email = 'christine.baert@demo.padel'),
     3, NULL,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '55 days', 0.00),
    ('L0003',
     (SELECT id_personne FROM personne WHERE email = 'antoine.demoulin@demo.padel'),
     3, NULL,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '5 days', 0.00)
ON CONFLICT (matricule) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 4: Disponibilités — 14 rolling days from CURRENT_DATE
--
-- Slot schedule mirrors DisponibiliteService logic (105-min step):
--   Site 1 (opens 09:00): 540, 645, 750, 855, 960, 1065, 1170 minutes → 7 slots/day
--     = 09:00, 10:45, 12:30, 14:15, 16:00, 17:45, 19:30
--   Site 2 (opens 10:00): 600, 705, 810, 915, 1020, 1125 minutes → 6 slots/day
--     = 10:00, 11:45, 13:30, 15:15, 17:00, 18:45
--
-- Totals:
--   Site 1: 6 terrains × 7 slots × 14 days =  588
--   Site 2: 6 terrains × 6 slots × 14 days =  504
--   Grand total: 1,092 rows
--
-- UNIQUE constraint: (date_heure_debut, id_terrain)
-- ─────────────────────────────────────────────────────────────────
INSERT INTO disponibilite (id_terrain, date_heure_debut, date_heure_fin, statut)
SELECT
    t.id_terrain,
    (CURRENT_DATE
        + gs_day * INTERVAL '1 day'
        + slots.offset_minutes * INTERVAL '1 minute')::TIMESTAMP AS date_heure_debut,
    (CURRENT_DATE
        + gs_day * INTERVAL '1 day'
        + slots.offset_minutes * INTERVAL '1 minute'
        + INTERVAL '90 minutes')::TIMESTAMP                       AS date_heure_fin,
    'LIBRE'
FROM terrain t
CROSS JOIN generate_series(0, 13) AS gs_day
CROSS JOIN LATERAL (
    SELECT unnest(
        CASE
            WHEN t.id_site = 1
                THEN ARRAY[540, 645, 750, 855, 960, 1065, 1170]::int[]
            ELSE
                ARRAY[600, 705, 810, 915, 1020, 1125]::int[]
        END
    ) AS offset_minutes
) AS slots
ON CONFLICT (date_heure_debut, id_terrain) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 5: Matchs
--
-- Slot lookup: JOIN disponibilite → terrain to match on
--   (id_site, terrain.numero, date_heure_debut).
-- date_heure_debut = CURRENT_DATE + n days + m minutes (must match
-- Section 4's generation formula exactly).
--
-- | # | Type   | Statut     | Site | Terrain | Day | Time  | Organisateur |
-- | 1 | PRIVE  | EN_ATTENTE |  1   |    1    | +2  | 10:45 | G0001        |
-- | 2 | PRIVE  | EN_ATTENTE |  1   |    2    | +3  | 14:15 | S0002        |
-- | 3 | PUBLIC | EN_ATTENTE |  1   |    3    | +4  | 09:00 | G0002        |
-- | 4 | PUBLIC | EN_ATTENTE |  2   |    1    | +2  | 10:00 | S0004        |
-- | 5 | PUBLIC | EN_ATTENTE |  2   |    2    | +5  | 13:30 | S0005        |
-- | 6 | PRIVE  | ANNULE     |  1   |    1    | +7  | 12:30 | S0001        |
-- | 7 | PUBLIC | ANNULE     |  2   |    1    | +6  | 15:15 | G0003        |
--
-- Dispo statut is set to RESERVE via bulk UPDATE below (EN_ATTENTE only).
-- ANNULE matches leave their dispo LIBRE (mirrors app annulation behaviour).
-- UNIQUE constraint on match_padel: id_dispo
-- ─────────────────────────────────────────────────────────────────

-- Match 1: PRIVE EN_ATTENTE — Site 1, Terrain 1, J+2 10:45, G0001
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0001', 'PRIVE', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '5 days'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 645 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 2: PRIVE EN_ATTENTE — Site 1, Terrain 2, J+3 14:15, S0002
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0002', 'PRIVE', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '3 days'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '3 days' + 855 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 3: PUBLIC EN_ATTENTE — Site 1, Terrain 3, J+4 09:00, G0002
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0002', 'PUBLIC', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '2 days'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 4: PUBLIC EN_ATTENTE — Site 2, Terrain 1, J+2 10:00, S0004
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0004', 'PUBLIC', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '4 days'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 600 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 5: PUBLIC EN_ATTENTE — Site 2, Terrain 2, J+5 13:30, S0005
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0005', 'PUBLIC', 'EN_ATTENTE', 60.00, NOW() - INTERVAL '1 day'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '5 days' + 810 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 6: PRIVE ANNULE — Site 1, Terrain 1, J+7 12:30, S0001
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'S0001', 'PRIVE', 'ANNULE', 60.00, NOW() - INTERVAL '6 days'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '7 days' + 750 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Match 7: PUBLIC ANNULE — Site 2, Terrain 1, J+6 15:15, G0003
INSERT INTO match_padel (id_dispo, matricule_organisateur, type_match, statut, montant_total, date_creation)
SELECT d.id_dispo, 'G0003', 'PUBLIC', 'ANNULE', 60.00, NOW() - INTERVAL '7 days'
FROM disponibilite d
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '6 days' + 915 * INTERVAL '1 minute'
ON CONFLICT (id_dispo) DO NOTHING;

-- Mark disponibilités for EN_ATTENTE matches as RESERVE (mirrors creerMatch service)
UPDATE disponibilite d
SET statut = 'RESERVE'
FROM match_padel m
WHERE m.id_dispo = d.id_dispo
  AND m.statut = 'EN_ATTENTE';


-- ─────────────────────────────────────────────────────────────────
-- Section 6: Participations
--
-- Replicates the organizer participation auto-inserted by creerMatch,
-- plus additional players per match.
-- Lookup mirrors Section 5: (id_site, terrain.numero, date_heure_debut).
--
-- | Match | Matricule | Statut     |
-- |   1   | G0001     | EN_ATTENTE | organizer
-- |   1   | G0002     | EN_ATTENTE | added by organizer (PRIVE)
-- |   2   | S0002     | EN_ATTENTE | organizer
-- |   3   | G0002     | EN_ATTENTE | organizer
-- |   3   | S0003     | CONFIRME   | inscrit (paid)
-- |   3   | L0002     | EN_ATTENTE | inscrit
-- |   4   | S0004     | EN_ATTENTE | organizer
-- |   4   | S0005     | CONFIRME   | inscrit (paid)
-- |   5   | S0005     | EN_ATTENTE | organizer
-- |   6   | S0001     | ANNULEE    | organizer (cancelled match)
-- |   7   | G0003     | ANNULEE    | organizer (cancelled match)
--
-- V11 partial unique index: (id_match, matricule) WHERE statut <> 'ANNULEE'
-- ─────────────────────────────────────────────────────────────────

-- Match 1 / G0001
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'G0001', 'EN_ATTENTE', NOW() - INTERVAL '5 days'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 645 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 1 / G0002
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'G0002', 'EN_ATTENTE', NOW() - INTERVAL '4 days 12 hours'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 645 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 2 / S0002
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0002', 'EN_ATTENTE', NOW() - INTERVAL '3 days'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '3 days' + 855 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 3 / G0002
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'G0002', 'EN_ATTENTE', NOW() - INTERVAL '2 days'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 3 / S0003 (CONFIRME — paid)
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0003', 'CONFIRME', NOW() - INTERVAL '1 day 18 hours'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 3 / L0002
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'L0002', 'EN_ATTENTE', NOW() - INTERVAL '1 day 6 hours'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 4 / S0004
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0004', 'EN_ATTENTE', NOW() - INTERVAL '4 days'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 600 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 4 / S0005 (CONFIRME — paid)
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0005', 'CONFIRME', NOW() - INTERVAL '3 days 6 hours'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 600 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 5 / S0005
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0005', 'EN_ATTENTE', NOW() - INTERVAL '1 day'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '5 days' + 810 * INTERVAL '1 minute'
ON CONFLICT (id_match, matricule) WHERE statut <> 'ANNULEE' DO NOTHING;

-- Match 6 / S0001 (ANNULEE — cancelled match; outside partial index, no ON CONFLICT)
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'S0001', 'ANNULEE', NOW() - INTERVAL '6 days'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '7 days' + 750 * INTERVAL '1 minute';

-- Match 7 / G0003 (ANNULEE — cancelled match; outside partial index, no ON CONFLICT)
INSERT INTO participation (id_match, matricule, statut, date_inscription)
SELECT m.id_match, 'G0003', 'ANNULEE', NOW() - INTERVAL '7 days'
FROM match_padel m
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '6 days' + 915 * INTERVAL '1 minute';


-- ─────────────────────────────────────────────────────────────────
-- Section 7: Paiements — 1 per participation (11 total)
--
-- montant = 60.00 / 4 = 15.00 (MatchPolicy.PRIX_PLACE_EUR)
-- solde_inclus = 0.00 (no credit balance used)
-- EN_ATTENTE participation → EN_ATTENTE paiement, date_paiement NULL
-- CONFIRME participation  → PAYE paiement, date_paiement set in past
-- ANNULEE participation   → ANNULE paiement, date_paiement NULL
--
-- UNIQUE constraint: id_participation (one paiement per participation)
-- ─────────────────────────────────────────────────────────────────

-- Match 1 / G0001 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 645 * INTERVAL '1 minute'
  AND p.matricule = 'G0001'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 1 / G0002 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 645 * INTERVAL '1 minute'
  AND p.matricule = 'G0002'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 2 / S0002 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '3 days' + 855 * INTERVAL '1 minute'
  AND p.matricule = 'S0002'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 3 / G0002 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
  AND p.matricule = 'G0002'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 3 / S0003 / PAYE (CONFIRME participation)
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NOW() - INTERVAL '20 hours', 'PAYE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
  AND p.matricule = 'S0003'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 3 / L0002 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 3
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '4 days' + 540 * INTERVAL '1 minute'
  AND p.matricule = 'L0002'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 4 / S0004 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 600 * INTERVAL '1 minute'
  AND p.matricule = 'S0004'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 4 / S0005 / PAYE (CONFIRME participation)
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NOW() - INTERVAL '2 days 14 hours', 'PAYE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '2 days' + 600 * INTERVAL '1 minute'
  AND p.matricule = 'S0005'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 5 / S0005 / EN_ATTENTE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'EN_ATTENTE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 2
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '5 days' + 810 * INTERVAL '1 minute'
  AND p.matricule = 'S0005'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 6 / S0001 / ANNULE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'ANNULE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 1 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '7 days' + 750 * INTERVAL '1 minute'
  AND p.matricule = 'S0001'
ON CONFLICT (id_participation) DO NOTHING;

-- Match 7 / G0003 / ANNULE
INSERT INTO paiement (id_participation, montant, solde_inclus, date_paiement, statut)
SELECT p.id_participation, 15.00, 0.00, NULL, 'ANNULE'
FROM participation p
JOIN match_padel m ON m.id_match = p.id_match
JOIN disponibilite d ON d.id_dispo = m.id_dispo
JOIN terrain t ON t.id_terrain = d.id_terrain
WHERE t.id_site = 2 AND t.numero = 1
  AND d.date_heure_debut = CURRENT_DATE + INTERVAL '6 days' + 915 * INTERVAL '1 minute'
  AND p.matricule = 'G0003'
ON CONFLICT (id_participation) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 8: Pénalité — 1 active penalty on S0002 (site 1 member)
--
-- penalite has no UNIQUE constraint (multiple penalties per member allowed).
-- Flyway checksum guarantees this runs exactly once.
-- ─────────────────────────────────────────────────────────────────
INSERT INTO penalite (matricule, date_debut, date_fin, motif)
VALUES (
    'S0002',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '25 days',
    'Abandon de match sans préavis (3ème occurrence)'
);


-- ═══════════════════════════════════════════════════════════════════
-- Section 9: Verification queries (copy-paste after migration)
--
-- docker exec -it $(docker ps -qf "ancestor=postgres") \
--   psql -U postgres -d padel_manager -c "
-- SELECT 'site'           AS tbl, COUNT(*) FROM site
-- UNION ALL SELECT 'terrain',         COUNT(*) FROM terrain
-- UNION ALL SELECT 'type_membre',     COUNT(*) FROM type_membre
-- UNION ALL SELECT 'personne',        COUNT(*) FROM personne
-- UNION ALL SELECT 'membre',          COUNT(*) FROM membre
-- UNION ALL SELECT 'administrateur',  COUNT(*) FROM administrateur
-- UNION ALL SELECT 'disponibilite',   COUNT(*) FROM disponibilite
-- UNION ALL SELECT 'match_padel',     COUNT(*) FROM match_padel
-- UNION ALL SELECT 'participation',   COUNT(*) FROM participation
-- UNION ALL SELECT 'paiement',        COUNT(*) FROM paiement
-- UNION ALL SELECT 'penalite',        COUNT(*) FROM penalite;
-- "
--
-- Expected counts:
--   site           →    2
--   terrain        →   12
--   type_membre    →    3
--   personne       →   12
--   membre         →   12
--   administrateur →    2
--   disponibilite  → 1092
--   match_padel    →    7
--   participation  →   11
--   paiement       →   11
--   penalite       →    1
-- ═══════════════════════════════════════════════════════════════════
