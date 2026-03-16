-- ═══════════════════════════════════════════════
-- V7 — Admin site de test
-- ═══════════════════════════════════════════════

-- Admin site 1 (mot de passe = "password")
INSERT INTO administrateur (email, nom, prenom, mot_de_passe, role, id_site)
VALUES ('admin.site@padel.be', 'Admin', 'Site',
        '$2a$10$CE/5N7xjBSNk4rKatgISp.abpfU4zqjWdf.uybvzxOqOrACNyb15u',
        'ADMIN_SITE', 1);
