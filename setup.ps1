# PadelManager — Configuration de l'environnement (Windows / PowerShell 5.1+)
# Génère backend\padel-manager\.env avec un JWT_SECRET aléatoire sécurisé.
# Usage : .\setup.ps1
# Idempotent : ne touche à rien si .env existe déjà.

$ErrorActionPreference = "Stop"

$envFile    = "backend\padel-manager\.env"
$envExample = "backend\padel-manager\.env.example"

# ---- Idempotence ----
if (Test-Path $envFile) {
    Write-Host "[setup] $envFile existe déjà — aucune action."
    Write-Host "[setup] Pour repartir de zéro : Remove-Item $envFile; .\setup.ps1"
    exit 0
}

# ---- Vérifier le template ----
if (-not (Test-Path $envExample)) {
    Write-Host "[setup] ERROR: $envExample introuvable."
    Write-Host "[setup] Vous devez exécuter ce script depuis la racine du dépôt."
    exit 1
}

# ---- Générer JWT_SECRET (48 octets cryptographiquement sûrs) ----
Write-Host "[setup] Génération du JWT_SECRET..."
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
} finally {
    $rng.Dispose()
}
$jwtSecret = [Convert]::ToBase64String($bytes)

# ---- Lire le template, remplacer les valeurs ----
Write-Host "[setup] Création de $envFile..."
$content = Get-Content $envExample -Raw

$content = $content -replace '(?m)^POSTGRES_USER=.*$',     'POSTGRES_USER=padel_user'
$content = $content -replace '(?m)^POSTGRES_PASSWORD=.*$', 'POSTGRES_PASSWORD=padel_dev_password'
$content = $content -replace '(?m)^POSTGRES_DB=.*$',       'POSTGRES_DB=padel_manager'
$content = $content -replace '(?m)^DB_USERNAME=.*$',       'DB_USERNAME=app_user'
$content = $content -replace '(?m)^DB_PASSWORD=.*$',       'DB_PASSWORD=padel_password'
$content = $content -replace '(?m)^FLYWAY_USER=.*$',       'FLYWAY_USER=padel_user'
$content = $content -replace '(?m)^FLYWAY_PASSWORD=.*$',   'FLYWAY_PASSWORD=padel_dev_password'
$content = $content -replace '(?m)^JWT_SECRET=.*$',        "JWT_SECRET=$jwtSecret"

Set-Content -Path $envFile -Value $content -NoNewline

Write-Host "[setup] OK — $envFile créé avec des valeurs de développement."
Write-Host ""
Write-Host "[setup] Prochaines étapes :"
Write-Host "  1. docker compose up -d"
Write-Host "  2. cd backend\padel-manager; .\mvnw.cmd spring-boot:run"
Write-Host "  3. cd frontend; npm install; npm start"
