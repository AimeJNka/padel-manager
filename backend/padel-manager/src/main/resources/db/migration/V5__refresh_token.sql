-- ═══════════════════════════════════════════════
-- V5 — Table refresh_token pour le mécanisme de renouvellement JWT
-- ═══════════════════════════════════════════════

CREATE TABLE refresh_token (
    id_refresh_token BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    matricule VARCHAR(10) NOT NULL REFERENCES membre(matricule),
    date_expiration TIMESTAMP NOT NULL,
    revoque BOOLEAN NOT NULL DEFAULT false,
    date_creation TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_matricule ON refresh_token(matricule);
CREATE INDEX idx_refresh_token_token ON refresh_token(token);

