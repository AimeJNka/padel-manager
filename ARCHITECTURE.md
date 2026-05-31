# Architecture — PadelManager

Application web full-stack de gestion de terrains de padel multi-sites,
développée comme projet académique (EPHEC, PDW & SGBD 2025-2026).

## 1. Vue d'ensemble

L'application gère deux sites physiques (**Padel Brussels Center** et **Padel Liège Arena**)
avec, pour chacun, un parc de terrains, des horaires d'ouverture, des créneaux générés
annuellement et un cycle de vie complet pour chaque match : création par un membre,
participation jusqu'à 4 joueurs, paiement individuel, démarrage automatisé, calcul du
solde, marquage comme effectué.

Trois rôles cohabitent :

- **Membre** — connexion par `matricule` (format `G####`, `L####`, `S####`), réserve
  ou rejoint des matchs, paie sa part, consulte ses pénalités.
- **ADMIN_SITE** — admin d'un site donné (`id_site` porté par le JWT), gère les créneaux,
  pénalités, horaires et fermetures de son site uniquement.
- **ADMIN_GLOBAL** — vue transverse sur tous les sites, paramétrage cross-site,
  statistiques globales.

## 2. Stack technique

| Couche       | Technologie         | Version  | Rôle                              |
|--------------|---------------------|----------|-----------------------------------|
| Frontend     | Angular             | 20.3     | SPA standalone components         |
|              | Angular Material    | 20.2     | dialogs, snackbar                 |
|              | Tailwind CSS        | 4.3      | utility-first styling             |
|              | RxJS                | 7.8      | HTTP streams                      |
| Backend      | Spring Boot         | 3.5.11   | REST API                          |
|              | Spring Security     | 6.x      | JWT auth (HS256)                  |
|              | Spring Data JPA     | -        | ORM (Hibernate)                   |
|              | JJWT                | 0.12.6   | signature et parsing des tokens   |
|              | Springdoc OpenAPI   | 2.8.5    | Swagger UI                        |
|              | Flyway              | -        | migrations versionnées            |
|              | spring-dotenv       | 4.0.0    | chargement des secrets depuis .env|
| Base données | PostgreSQL          | 16       | données relationnelles            |
| Build / CI   | Maven Wrapper       | -        | build backend                     |
|              | npm + Angular CLI   | 20.3     | build frontend                    |
|              | JaCoCo              | 0.8.12   | couverture backend                |
|              | GitHub Actions      | -        | intégration continue              |
| Infra dev    | Docker Compose      | -        | Postgres local                    |
| Runtime      | Java                | 21 LTS   |                                   |
|              | Node                | 20+      |                                   |

## 3. Architecture backend

### 3.1 Découpage en couches

Le module Maven `backend/padel-manager/` suit le découpage classique Spring Boot :

```
Controller (REST) → Service (Interface + Impl) → Repository (Spring Data) → Entity (JPA)
```

Convention DTO **non négociable** : aucun contrôleur n'expose une entité JPA
directement. Chaque entité a un DTO (input et output séparés quand pertinent),
mappé via une méthode statique `from(Entity)` sur le DTO.

### 3.2 Inventaire des packages

```
be.ephec.padelmanager/
├── PadelManagerApplication.java     entry point
├── config/                          SecurityConfig, CorsConfig, JwtAuthFilter,
│                                    GlobalExceptionHandler, Role, MatchPolicy
├── controller/                      13 contrôleurs REST (préfixe /api)
├── dto/                             DTOs request/response, sous-package auth/
├── exception/                       exceptions métier
├── model/                           15 entités JPA
├── repository/                      interfaces Spring Data JPA
├── scheduler/                       MatchScheduler (cron horaire)
└── service/
    ├── IXxxService.java             interfaces métier
    ├── SiteAccessChecker.java       helper transverse pour authz site-scopée
    └── impl/                        implémentations
```

### 3.3 Sécurité

- **Access token** : JWT signé HS256, 15 min de durée (membre), 2 h (admin).
  Claims utilisés : `sub` (matricule pour les membres, email pour les admins),
  `role` (`MEMBRE`, `ADMIN_SITE`, `ADMIN_GLOBAL`), `idSite` (présent uniquement
  pour les admins de site).
- **Refresh token** : opaque UUID persisté en DB (`refresh_token`), 7 jours.
  Révoqué à chaque login pour invalider les tokens des sessions précédentes.
- **Filtre JWT** : `JwtAuthFilter` extrait le token de l'en-tête `Authorization`,
  valide la signature et la fraîcheur, et place `idSite` dans `Authentication.details`.
- **Vérification site-scopée** : `SiteAccessChecker` lit `Authentication.details`
  et compare au site de la ressource demandée. Injecté par `PaiementService` et
  `PenaliteService`.
- **SecurityConfig** :
  - session stateless, CSRF désactivé, CORS configuré
  - routes publiques : `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`,
    `/api/auth/admin/login`, `/api/health`, `GET /api/types-membres`, `GET /api/sites`,
    `/swagger-ui/**`, `/v3/api-docs/**`
  - tout le reste : `.anyRequest().authenticated()`
- **Authorisation au niveau méthode** : `@PreAuthorize("hasRole('ADMIN_GLOBAL')")`
  ou `hasAnyRole('ADMIN_GLOBAL', 'ADMIN_SITE')` selon le besoin.

### 3.4 Génération de créneaux

Les créneaux (`disponibilite`) sont **précalculés annuellement** par site, depuis
les horaires d'ouverture annuels et les fermetures ponctuelles / récurrentes.

- Durée d'un créneau : **90 min**.
- Pause inter-créneau : **15 min**.
- Pas entre deux débuts de créneau : **105 min**.
- Endpoints admin :
  - `POST /api/admin/creneaux/generer` — première génération pour une année donnée.
  - `POST /api/admin/creneaux/regenerer` — re-génération : seuls les créneaux
    `LIBRE` non référencés par un match sont supprimés ; les créneaux `RESERVE`
    et ceux référencés par un match sont préservés (clé composite terrain + début).
- Les fermetures avec `id_site = NULL` s'appliquent à **tous** les sites.

### 3.5 Scheduler

`MatchScheduler` (`@Scheduled` cron horaire, top de chaque heure) orchestre 4 jobs
séquentiels :

| Job | Description |
|-----|-------------|
| 1 | Bascule les matchs `PRIVE` non remplis en `PUBLIC` quand l'échéance d'inscription est dépassée. |
| 2 | Libère les participations non payées 24 h avant le début (récupère les places). |
| 3 | Marque les matchs `PUBLIC`/`PRIVE` en `DEMARRE` et calcule le solde des participations payées. |
| 4 | Marque les matchs `DEMARRE` en `EFFECTUE` une fois leur durée écoulée. |

Chaque job est transactionnel et idempotent ; un job qui ne trouve rien à traiter
logge `0` traité et termine.

### 3.6 Migrations Flyway

Toutes sous `src/main/resources/db/migration/`, exécutées au démarrage du backend
si la base est vide ou en retard.

| Version | Rôle |
|---------|------|
| V1 | Schéma du domaine (15 tables) |
| V2 | Utilisateurs DB et rôles |
| V3 | Données de référence : `type_membre`, sites, terrains, comptes de test |
| V4 | Grants pour V1 |
| V5 | Table `refresh_token` |
| V6 | Grants pour V5 |
| V7 | Comptes admin pour les sites (`admin.site@padel.be`, `admin.site2@padel.be`) |
| V8 | Contraintes paiement (M8) |
| V9 | `paiement.date_paiement` nullable (M8) |
| V10 | Lifecycle match + check constraints |
| V11 | Index unique partiel sur `participation` |
| V12 | Seed de données démo (matchs, participations, paiements) |
| V13 | Ajout du statut `EFFECTUE` |
| V14 | Seed admin site 2 et données démo additionnelles |

Le `membre.matricule` est une PK `VARCHAR` mutable — à éviter en clé étrangère
dans tout nouveau développement (préférer `id_personne`).

## 4. Architecture frontend

### 4.1 Structure

```
frontend/src/app/
├── core/
│   ├── guards/             authGuard, roleGuard
│   ├── interceptors/       injection du header Authorization, retry sur 401
│   ├── models/             DTOs typés (miroir des DTOs backend)
│   └── services/           11 services HttpClient (auth, membre, match,
│                           disponibilite, paiement, penalite, site, terrain,
│                           horaire, fermeture-ponctuelle, fermeture-recurrente,
│                           token-storage)
├── shared/
│   ├── components/         kpi-card, status-badge, match-card, page-shell, coming-soon
│   └── dialogs/            confirm-dialog, creer-match-dialog
└── features/
    ├── admin/              espaces admin_site et admin_global (dashboard, membres,
    │                       créneaux, horaires, fermetures, paiements, pénalités)
    ├── auth/               login (matricule), admin-login (email), register
    ├── dashboard/          accueil membre
    ├── historique/         historique des matchs joués
    ├── landing/            page publique
    ├── matchs/             créer / rejoindre / lister les matchs
    ├── paiements/          mes paiements + écrans admin
    └── penalites/          mes pénalités + écrans admin
```

### 4.2 Routing et guards

Routes déclarées dans `app.routes.ts` avec `loadComponent` (lazy par composant
standalone). `authGuard` bloque les routes protégées si pas de token valide.
`roleGuard` vérifie le rôle issu du JWT décodé (`ADMIN_GLOBAL`, `ADMIN_SITE`).
Les routes publiques (`/login`, `/admin/login`, `/register`, `/landing`)
n'invoquent aucun guard.

### 4.3 Gestion d'état

État local par composant via les **signals** d'Angular (`signal()`, `computed()`).
`ChangeDetectionStrategy.OnPush` par défaut. Aucun store global (pas de NgRx).
Les services HttpClient retournent des `Observable` ; conversion en signal via
`toSignal()` côté composant si la valeur doit alimenter le template.

### 4.4 HTTP et proxy

Tous les services utilisent des URLs relatives (`/api/...`). Le proxy de dev
(`frontend/proxy.conf.json`) redirige `/api/*` vers `http://localhost:8080` :

```json
{ "/api": { "target": "http://localhost:8080", "secure": false,
            "changeOrigin": true, "logLevel": "debug" } }
```

L'`AuthService` est le seul service stateful : il maintient les signals
`accessToken`, `refreshToken`, `role`, `idSite`, `matricule`, dérivés du JWT
décodé. La méthode `refresh()` est **réactive** — déclenchée par un 401 dans
l'intercepteur ou par `hydrate()` au démarrage. Pas de timer auto-refresh.

### 4.5 Design system

- **Tailwind v4** via `@import "tailwindcss"` dans `src/styles.css`, avec tokens
  custom dans `@theme` (couleurs `padel-blue` / `padel-lime` / `padel-ink`,
  ombres, font stack).
- **Material M3** conservé pour les overlays (dialogs, snackbar) via
  `src/custom-theme.scss`. Le `body` restaure la typographie Material après le
  preflight Tailwind.
- Primitives partagées : classes `.field` / `.input` (form controls),
  composant `app-status-badge` (statuts colorés monospace), `app-kpi-card`,
  `app-page-shell`.

## 5. Base de données

15 tables de domaine (+ `flyway_schema_history`). Le schéma logique complet est
documenté dans le cahier des charges (`docs/cdc/CDC_GestionTerrainPadle_.pdf`).

Particularités notables :

- `membre.matricule` est une PK `VARCHAR` mutable (héritage CDC).
- Un trigger DB empêche d'insérer plus de **4 participations** par match.
- Le lifecycle d'un match est verrouillé par des contraintes `CHECK` (V10) et
  un index unique partiel sur `participation` (V11) qui empêche un même membre
  d'occuper deux fois la même place dans un match `PRIVE`/`PUBLIC` ouvert.

L'application se connecte en tant que superuser `postgres`. Un utilisateur
`app_user` à privilèges limités est défini en V2 mais n'est pas encore câblé
au datasource ; objectif post-jury.

## 6. Documentation API

Une fois le backend démarré :

- Swagger UI : `http://localhost:8080/swagger-ui`
- Spec OpenAPI JSON : `http://localhost:8080/v3/api-docs`

Les opérations sont triées par méthode HTTP au sein de chaque tag (classe contrôleur),
les tags eux-mêmes sont triés alphabétiquement, et la durée de chaque requête est
affichée — réglages via le bloc `springdoc:` dans `application.yml`.

## 7. Tests et qualité

- **Backend** : 204 tests JUnit (unitaires + intégration via Testcontainers).
  `./mvnw test` en ~1 minute. JaCoCo génère un rapport HTML dans
  `target/site/jacoco/index.html` à chaque run.
- **Frontend** : specs unitaires pour les services HTTP critiques
  (`auth.service`, `disponibilite.service`, `paiement.service`, `penalite.service`,
  `membre.service`) plus `app.spec.ts` pour la coquille de routing. Toutes via
  `provideHttpClientTesting()` (Angular 20 idiomatic). Exécution headless :
  `npm run test:ci`.
- **CI** : `.github/workflows/ci.yml` lance deux jobs en parallèle :
  - `backend` — `./mvnw test`, JaCoCo, résumé enrichi (badges, couverture par package)
  - `frontend` — `npm ci`, `npm run build`, `npm run test:ci`
  Trigger : tout push (`branches: ["**"]`) et toute PR vers `main`.

## 8. Décisions et contraintes notables

- **Convention DTO** non négociable : pas d'entité JPA dans une réponse HTTP.
- **PK `matricule`** historiquement mutable : ne pas y attacher de FK dans
  un nouveau développement.
- **Secrets** chargés via `spring-dotenv` depuis `backend/padel-manager/.env`
  (cf. `EXPLOITATION.md`). L'application **ne démarre pas** sans ce fichier.
- **Génération de créneaux** atomique à l'année : pas d'incrémental.
- **Refresh token** réactif uniquement : pas de timer client.
- **Tailwind v4 + Material M3** cohabitent en restaurant les tokens Material
  après le preflight Tailwind.
