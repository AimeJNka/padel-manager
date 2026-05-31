-- ═══════════════════════════════════════════════════════════════════
-- V14 — Seed: missing site 2 admin + 2027 horaires + 4 extra members
--
-- Purpose:
--   1. Fill the ADMIN_SITE gap for id_site=2 (Padel Liège Arena).
--      V7 added an admin only for site 1; site 2 had none.
--   2. Configure horaires for 2027 on both sites so Sprint 2
--      (Génération créneaux) can target year=2027 without first
--      needing a separate horaire setup.
--   3. Add 4 demo members across types (G0100/S0100/S0101/L0100)
--      for diversity on Sprint 3's /admin/membres list.
--
-- All INSERTs use ON CONFLICT DO NOTHING for idempotence on replay.
-- Password hash: BCrypt("password", strength=10) — same as V3/V7/V12.
-- Matricule range 0100+ chosen to skip past V3/V12 seeds AND any
-- organic accounts created via the registration UI (currently up
-- to G0005 / S0007 / L0003).
-- ═══════════════════════════════════════════════════════════════════


-- ─────────────────────────────────────────────────────────────────
-- Section 1: Missing ADMIN_SITE for Liège (id_site=2)
-- ─────────────────────────────────────────────────────────────────
INSERT INTO administrateur (email, nom, prenom, mot_de_passe, role, id_site) VALUES
    ('admin.site2@padel.be', 'Admin', 'Liège',
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     'ADMIN_SITE', 2)
ON CONFLICT (email) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 2: Horaires 2027 (both sites, mirror of V3's 2026 schedule)
-- UNIQUE (annee, id_site) per V1
-- ─────────────────────────────────────────────────────────────────
INSERT INTO horaire_annuel (id_site, annee, heure_ouverture, heure_fermeture) VALUES
    (1, 2027, '09:00', '22:00'),
    (2, 2027, '10:00', '21:00')
ON CONFLICT (annee, id_site) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────
-- Section 3: Extra demo members (4 rows for Sprint 3 diversity)
-- Range G0100/S0100/S0101/L0100 avoids collision with seed + organic
-- ─────────────────────────────────────────────────────────────────

-- 3a. Persons (4 rows, distinct emails @v14.padel)
INSERT INTO personne (email, nom, prenom, telephone) VALUES
    ('marie.dubois@v14.padel',    'Dubois',   'Marie',  '+32 478 11 22 33'),
    ('claude.boucher@v14.padel',  'Boucher',  'Claude', '+32 478 11 22 34'),
    ('chloe.morel@v14.padel',     'Morel',    'Chloé',  '+32 478 11 22 35'),
    ('samuel.gauthier@v14.padel', 'Gauthier', 'Samuel', '+32 478 11 22 36')
ON CONFLICT (email) DO NOTHING;

-- 3b. Members (4 rows, id_personne resolved by email subquery)
-- S0101 has solde_du = 30.00 to demonstrate "due balance" UI in Sprint 3.
INSERT INTO membre (matricule, id_personne, id_type, id_site, mot_de_passe, date_inscription, solde_du) VALUES
    ('G0100',
     (SELECT id_personne FROM personne WHERE email = 'marie.dubois@v14.padel'),
     1, NULL,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '14 days', 0.00),

    ('S0100',
     (SELECT id_personne FROM personne WHERE email = 'claude.boucher@v14.padel'),
     2, 1,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '10 days', 0.00),

    ('S0101',
     (SELECT id_personne FROM personne WHERE email = 'chloe.morel@v14.padel'),
     2, 2,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '5 days', 30.00),

    ('L0100',
     (SELECT id_personne FROM personne WHERE email = 'samuel.gauthier@v14.padel'),
     3, NULL,
     '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
     CURRENT_DATE - INTERVAL '3 days', 0.00)
ON CONFLICT (matricule) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════════
-- Verification queries (copy-paste after Flyway applies V14):
--
-- SELECT * FROM administrateur ORDER BY id_admin;
--   Expected: 3 rows (admin global + admin site 1 + NEW admin.site2@padel.be)
--
-- SELECT * FROM horaire_annuel WHERE annee = 2027 ORDER BY id_site;
--   Expected: 2 rows (site 1: 09:00-22:00 / site 2: 10:00-21:00)
--
-- SELECT matricule, nom, prenom, id_site, solde_du FROM membre
--   WHERE matricule IN ('G0100','S0100','S0101','L0100') ORDER BY matricule;
--   Expected: 4 rows. S0101 has solde_du = 30.00; others 0.00.
--
-- Login test (cleartext = "password"):
--   admin.site2@padel.be → admin dashboard, greeting "Administrateur de Padel Liège Arena"
--   G0100, S0100, S0101, L0100 → member dashboard
-- ═══════════════════════════════════════════════════════════════════
