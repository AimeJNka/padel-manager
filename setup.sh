#!/usr/bin/env bash
# PadelManager — Configuration de l'environnement (macOS / Linux / WSL / Git Bash)
# Génère backend/padel-manager/.env avec un JWT_SECRET aléatoire sécurisé.
# Usage : ./setup.sh
# Idempotent : ne touche à rien si .env existe déjà.

set -e

ENV_FILE="backend/padel-manager/.env"
ENV_EXAMPLE="backend/padel-manager/.env.example"

# ---- Idempotence ----
if [ -f "$ENV_FILE" ]; then
  echo "[setup] $ENV_FILE existe déjà — aucune action."
  echo "[setup] Pour repartir de zéro : rm $ENV_FILE && ./setup.sh"
  exit 0
fi

# ---- Vérifier le template ----
if [ ! -f "$ENV_EXAMPLE" ]; then
  echo "[setup] ERROR: $ENV_EXAMPLE introuvable."
  echo "[setup] Vous devez exécuter ce script depuis la racine du dépôt."
  exit 1
fi

# ---- Générer JWT_SECRET (48 octets -> ~64 caractères base64, > minimum HS256 de 32) ----
echo "[setup] Génération du JWT_SECRET..."
if command -v openssl >/dev/null 2>&1; then
  JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
elif [ -r /dev/urandom ]; then
  JWT_SECRET=$(head -c 48 /dev/urandom | base64 | tr -d '\n')
else
  echo "[setup] ERROR: ni openssl ni /dev/urandom disponible. Impossible de générer un secret."
  exit 1
fi

# ---- Copier le template ----
echo "[setup] Création de $ENV_FILE..."
cp "$ENV_EXAMPLE" "$ENV_FILE"

# ---- Remplacer les valeurs (sed -i.bak puis rm : portable BSD + GNU sed) ----
sed -i.bak "s|^POSTGRES_USER=.*|POSTGRES_USER=padel_user|"                 "$ENV_FILE"
sed -i.bak "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=padel_dev_password|" "$ENV_FILE"
sed -i.bak "s|^POSTGRES_DB=.*|POSTGRES_DB=padel_manager|"                  "$ENV_FILE"
sed -i.bak "s|^DB_USERNAME=.*|DB_USERNAME=app_user|"                       "$ENV_FILE"
sed -i.bak "s|^DB_PASSWORD=.*|DB_PASSWORD=padel_password|"                 "$ENV_FILE"
sed -i.bak "s|^FLYWAY_USER=.*|FLYWAY_USER=padel_user|"                     "$ENV_FILE"
sed -i.bak "s|^FLYWAY_PASSWORD=.*|FLYWAY_PASSWORD=padel_dev_password|"     "$ENV_FILE"
sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|"                      "$ENV_FILE"
rm -f "$ENV_FILE.bak"

echo "[setup] OK — $ENV_FILE créé avec des valeurs de développement."
echo ""
echo "[setup] Prochaines étapes :"
echo "  1. docker compose up -d"
echo "  2. cd backend/padel-manager && ./mvnw spring-boot:run"
echo "  3. cd frontend && npm install && npm start"
