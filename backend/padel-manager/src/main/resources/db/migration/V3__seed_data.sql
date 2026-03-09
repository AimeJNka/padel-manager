-- ═══════════════════════════════════════════════
-- V3 — Données de test
-- ═══════════════════════════════════════════════

-- Types de membres
INSERT INTO type_membre (prefixe, libelle, delai_reservation_jours, peut_creer_match)
VALUES
    ('G', 'Global',  21, true),
    ('S', 'Site',    14, true),
    ('L', 'Libre',    0, false);

-- Sites
INSERT INTO site (nom, adresse, ville) VALUES
                                           ('Padel Brussels Center', 'Rue de la Loi 1', 'Bruxelles'),
                                           ('Padel Liège Arena',     'Boulevard Sauvenière 10', 'Liège');

-- Terrains
INSERT INTO terrain (id_site, numero, statut) VALUES
                                                  (1, 1, 'ACTIF'), (1, 2, 'ACTIF'), (1, 3, 'ACTIF'),
                                                  (2, 1, 'ACTIF'), (2, 2, 'ACTIF');

-- Horaires 2026
INSERT INTO horaire_annuel (id_site, annee, heure_ouverture, heure_fermeture) VALUES
                                                                                  (1, 2026, '09:00', '22:00'),
                                                                                  (2, 2026, '10:00', '21:00');

-- Fermetures globales (1er janvier et 25 décembre)
INSERT INTO fermeture_ponctuelle (id_site, date_fermeture, motif) VALUES
                                                                      (NULL, '2026-01-01', 'Jour de l''an'),
                                                                      (NULL, '2026-12-25', 'Noël');

-- Fermeture récurrente site 2 (fermé le lundi)
INSERT INTO fermeture_recurrente (id_site, jour_semaine, motif) VALUES
    (2, 0, 'Jour de repos hebdomadaire');

-- Personnes de test
INSERT INTO personne (email, nom, prenom) VALUES
                                              ('global@test.be', 'Dupont', 'Jean'),
                                              ('site@test.be',   'Martin', 'Sophie'),
                                              ('libre@test.be',  'Durand', 'Marc');

-- Membres de test (mots de passe = "password" hashé BCrypt)
INSERT INTO membre (matricule, id_personne, id_type, id_site, mot_de_passe)
VALUES
    ('G0001', 1, 1, NULL,
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    ('S0001', 2, 2, 1,
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    ('L0001', 3, 3, NULL,
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- Admin global de test
INSERT INTO administrateur (email, nom, prenom, mot_de_passe, role, id_site)
VALUES
    ('admin@padel.be', 'Admin', 'Global',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'ADMIN_GLOBAL', NULL);