-- ═══════════════════════════════════════════════
-- V1 — Schéma initial Padel Manager
-- ═══════════════════════════════════════════════

CREATE TABLE type_membre (
                             id_type SERIAL PRIMARY KEY,
                             prefixe CHAR(1) NOT NULL UNIQUE,
                             libelle VARCHAR(50) NOT NULL,
                             delai_reservation_jours INTEGER NOT NULL,
                             peut_creer_match BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE personne (
                          id_personne SERIAL PRIMARY KEY,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          nom VARCHAR(100) NOT NULL,
                          prenom VARCHAR(100) NOT NULL,
                          telephone VARCHAR(20)
);

CREATE TABLE site (
                      id_site SERIAL PRIMARY KEY,
                      nom VARCHAR(150) NOT NULL,
                      adresse VARCHAR(255) NOT NULL,
                      ville VARCHAR(100) NOT NULL,
                      actif BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE membre (
                        matricule VARCHAR(10) PRIMARY KEY,
                        id_personne INTEGER NOT NULL REFERENCES personne(id_personne),
                        id_type INTEGER NOT NULL REFERENCES type_membre(id_type),
                        id_site INTEGER REFERENCES site(id_site),
                        mot_de_passe VARCHAR(255) NOT NULL,
                        date_inscription DATE NOT NULL DEFAULT CURRENT_DATE,
                        solde_du DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE administrateur (
                                id_admin SERIAL PRIMARY KEY,
                                id_site INTEGER REFERENCES site(id_site),
                                email VARCHAR(255) NOT NULL UNIQUE,
                                nom VARCHAR(100) NOT NULL,
                                prenom VARCHAR(100) NOT NULL,
                                mot_de_passe VARCHAR(255) NOT NULL,
                                role VARCHAR(20) NOT NULL
);

CREATE TABLE terrain (
                         id_terrain SERIAL PRIMARY KEY,
                         id_site INTEGER NOT NULL REFERENCES site(id_site),
                         numero INTEGER NOT NULL,
                         statut VARCHAR(10) NOT NULL DEFAULT 'ACTIF',
                         UNIQUE (numero, id_site)
);

CREATE TABLE horaire_annuel (
                                id_horaire SERIAL PRIMARY KEY,
                                id_site INTEGER NOT NULL REFERENCES site(id_site),
                                annee INTEGER NOT NULL,
                                heure_ouverture TIME NOT NULL,
                                heure_fermeture TIME NOT NULL,
                                UNIQUE (annee, id_site)
);

CREATE TABLE fermeture_recurrente (
                                      id_fermeture_rec SERIAL PRIMARY KEY,
                                      id_site INTEGER REFERENCES site(id_site),
                                      jour_semaine INTEGER NOT NULL,
                                      motif VARCHAR(255)
);

CREATE TABLE fermeture_ponctuelle (
                                      id_fermeture_ponc SERIAL PRIMARY KEY,
                                      id_site INTEGER REFERENCES site(id_site),
                                      date_fermeture DATE NOT NULL,
                                      motif VARCHAR(255) NOT NULL
);

CREATE TABLE disponibilite (
                               id_dispo SERIAL PRIMARY KEY,
                               id_terrain INTEGER NOT NULL REFERENCES terrain(id_terrain),
                               date_heure_debut TIMESTAMP NOT NULL,
                               date_heure_fin TIMESTAMP NOT NULL,
                               statut VARCHAR(10) NOT NULL DEFAULT 'LIBRE',
                               UNIQUE (date_heure_debut, id_terrain)
);

CREATE TABLE match_padel (
                             id_match SERIAL PRIMARY KEY,
                             id_dispo INTEGER NOT NULL UNIQUE REFERENCES disponibilite(id_dispo),
                             matricule_organisateur VARCHAR(10) NOT NULL REFERENCES membre(matricule),
                             type_match VARCHAR(10) NOT NULL,
                             statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
                             montant_total DECIMAL(10,2) NOT NULL DEFAULT 60.00,
                             date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE participation (
                               id_participation SERIAL PRIMARY KEY,
                               id_match INTEGER NOT NULL REFERENCES match_padel(id_match),
                               matricule VARCHAR(10) NOT NULL REFERENCES membre(matricule),
                               statut VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE',
                               date_inscription TIMESTAMP NOT NULL DEFAULT NOW(),
                               UNIQUE (id_match, matricule)
);

CREATE TABLE paiement (
                          id_paiement SERIAL PRIMARY KEY,
                          id_participation INTEGER NOT NULL UNIQUE REFERENCES participation(id_participation),
                          montant DECIMAL(10,2) NOT NULL,
                          solde_inclus DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                          date_paiement TIMESTAMP NOT NULL DEFAULT NOW(),
                          statut VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE'
);

CREATE TABLE penalite (
                          id_penalite SERIAL PRIMARY KEY,
                          matricule VARCHAR(10) NOT NULL REFERENCES membre(matricule),
                          date_debut TIMESTAMP NOT NULL DEFAULT NOW(),
                          date_fin TIMESTAMP NOT NULL,
                          motif VARCHAR(255) NOT NULL
);

-- ── Trigger : max 4 joueurs par match ──────────
CREATE OR REPLACE FUNCTION check_max_joueurs()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT COUNT(*) FROM participation
        WHERE id_match = NEW.id_match
          AND statut != 'ANNULEE') >= 4 THEN
        RAISE EXCEPTION 'Un match ne peut pas avoir plus de 4 joueurs';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_participation_max_joueurs
    BEFORE INSERT ON participation
    FOR EACH ROW EXECUTE FUNCTION check_max_joueurs();