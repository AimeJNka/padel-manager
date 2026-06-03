# PadelManager

[![CI](https://github.com/AimeJNka/padel-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/AimeJNka/padel-manager/actions/workflows/ci.yml)

Application web full-stack de gestion de terrains de padel multi-sites :
création de matchs, paiements individuels, pénalités, génération annuelle de
créneaux, tableaux de bord administrateurs. Projet académique EPHEC, PDW
2025-2026.

## Stack

| Couche     | Technologies                                                  |
|------------|---------------------------------------------------------------|
| Frontend   | Angular 20 (standalone) + Material 20 + Tailwind v4           |
| Backend    | Spring Boot 3.5 (Java 21) + Spring Security + Spring Data JPA |
| Base       | PostgreSQL 16 + Flyway                                        |
| API doc    | Springdoc OpenAPI / Swagger UI                                |
| Infra      | Docker Compose + GitHub Actions                               |

> Sécurité base de données — **moindre privilège** : le runtime se connecte via
> un rôle PostgreSQL à droits DML seulement (`app_user`), tandis que les
> migrations Flyway utilisent un rôle superuser distinct (`FLYWAY_USER`).
> Détails dans [ARCHITECTURE.md](ARCHITECTURE.md) (§5).

## Démarrage rapide

```bash
git clone https://github.com/AimeJNka/padel-manager.git
cd padel-manager
./setup.sh                                            # ou .\setup.ps1 sous Windows
docker compose up -d                                  # Postgres
cd backend/padel-manager && ./mvnw spring-boot:run &  # API sur :8080
cd frontend && npm install && npm start               # SPA sur :4200
```

Puis ouvrir http://localhost:4200.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — vue d'ensemble, couches, librairies,
  sécurité, scheduler, migrations.
- [EXPLOITATION.md](EXPLOITATION.md) — installation détaillée, tests, comptes
  de test, troubleshooting.
- [Cahier des charges](docs/cdc/CDC_GestionTerrainPadle_.pdf) — spécification
  fonctionnelle complète.

## API

- Swagger UI : http://localhost:8080/swagger-ui
- Spec OpenAPI : http://localhost:8080/v3/api-docs

## Tests

- Backend : 204 tests (`./mvnw test`, ~1 min, Testcontainers pour l'intégration)
- Frontend : services + build (`npm run test:ci`, `npm run build`)
- CI : GitHub Actions sur chaque push et PR vers `main` (jobs backend + frontend
  en parallèle)

## Comptes de test (mot de passe : `password`)

| Rôle         | Identifiant              | Connexion          |
|--------------|--------------------------|--------------------|
| ADMIN_GLOBAL | `admin@padel.be`         | `/admin/login`     |
| ADMIN_SITE   | `admin.site@padel.be`    | `/admin/login`     |
| Membre       | `G0001`                  | `/login`           |

Liste complète et détails dans [EXPLOITATION.md, §5](EXPLOITATION.md#5-comptes-de-test).

## Équipe

Aimé Nkurunziza — EPHEC, PDW & SGBD 2025-2026.
