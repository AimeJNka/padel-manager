# ADR-001 — Sessions administrateurs sans refresh token

## Statut

Accepté — 2026-05-12

## Contexte

L'application Padel Manager distingue deux catégories d'utilisateurs authentifiés :

- **Membres** (Global `G`, Site `S`, Libre `L`) : utilisateurs standards qui réservent et participent à des matchs.
- **Administrateurs** (Global, Site) : utilisateurs privilégiés qui configurent les sites, gèrent les terrains, les horaires annuels, les fermetures, génèrent les créneaux de disponibilité et consultent les statistiques.

L'authentification repose sur des JWT signés côté backend (Spring Boot) et stockés côté frontend (Angular) dans `localStorage` sous les clés `pm_access_token`, `pm_refresh_token`, `pm_role` et `pm_id_site`.

Pour les sessions membres, le backend émet à la connexion :

- un **access token** de courte durée (15 minutes par défaut) ;
- un **refresh token** de longue durée (7 jours par défaut), persisté en base dans la table `RefreshToken`.

Le frontend utilise le refresh token pour obtenir un nouvel access token sans re-authentification, ce qui permet une session continue sur plusieurs jours.

Pour les sessions administrateurs, le backend émet uniquement un **access token**. Aucun refresh token n'est généré, retourné, ni stocké. Le DTO `AdminAuthResponseDTO` ne contient pas de champ `refreshToken`.

## Décision

Les sessions administrateurs **n'utilisent intentionnellement pas de refresh token**.

L'access token administrateur a une durée de vie distincte de celle de l'access token membre :

- Access token **membre** : 15 minutes (`jwt.expiration`)
- Access token **administrateur** : 2 heures (`jwt.admin-expiration`)
- Refresh token (membre uniquement) : 7 jours (`jwt.refresh-expiration`)

À l'expiration de l'access token administrateur, l'utilisateur est redirigé vers `/login` et doit s'authentifier à nouveau.

## Justification

### 1. Principe de moindre privilège dans le temps

Les sessions privilégiées doivent être plus courtes que les sessions standards. C'est une recommandation explicite de l'OWASP ASVS (Application Security Verification Standard) : un compromis sur une session admin a un impact bien plus élevé qu'un compromis sur une session membre, donc la fenêtre d'exposition doit être réduite.

Une session administrateur qui se renouvelle automatiquement pendant 7 jours via un refresh token volé donne à un attaquant un accès privilégié prolongé. À l'inverse, une session bornée à 2 heures limite mécaniquement cette exposition.

### 2. Adéquation avec les cas d'usage administrateur

L'administration n'est pas une activité continue. Les tâches admin sont effectuées par sessions discrètes :

- création d'un site et de ses terrains
- définition des horaires annuels
- ajout de fermetures récurrentes ou ponctuelles
- génération annuelle des créneaux
- consultation périodique des statistiques

La durée de 2 heures couvre largement le cas d'usage le plus long (configuration initiale d'un site avec génération des créneaux pour l'année). Une session de plusieurs jours n'a pas de justification métier.

### 3. Réduction de la surface de maintenance et d'attaque

L'application possède déjà un flux de refresh côté membre. Dupliquer ce flux côté admin impliquerait :

- une table de refresh tokens admin séparée (ou une colonne discriminante)
- une logique de rotation et de révocation parallèle
- un risque d'incohérence entre les deux flux lors de futures évolutions

Conserver un seul flux de refresh, exclusivement membre, simplifie la maintenance et réduit la surface d'attaque.

### 4. Différenciation cohérente des durées

Le ratio choisi (admin 2h sans refresh / membre 15min + refresh 7 jours) crée une cohérence défendable :

- l'admin a un access token continu **8 fois plus long** que celui du membre ;
- mais la session totale admin reste **84 fois plus courte** que la session totale membre ;
- aucun mécanisme silencieux ne prolonge la session admin au-delà de la durée affichée du token.

## Conséquences

### Positives

- Conformité avec les bonnes pratiques de sécurité (OWASP ASVS).
- Surface d'attaque réduite côté backend (pas de gestion de refresh admin).
- Comportement explicite et auditable : la durée de vie de la session admin est exactement la durée de l'access token.
- Cohérent avec le `AdminAuthResponseDTO` qui n'expose pas de refresh token (vérifié dans `backend/padel-manager/src/main/java/be/ephec/padelmanager/dto/auth/AdminAuthResponseDTO.java`).

### Négatives / arbitrages assumés

- L'administrateur doit se reconnecter toutes les 2 heures en cas d'usage continu.
- Aucune notification UX explicite n'est implémentée à ce stade lors de l'expiration : l'utilisateur est simplement redirigé vers `/login`. Une amélioration UX (toast ou message contextuel "Votre session administrateur a expiré") est envisageable dans un sprint cosmétique ultérieur, hors scope de cette décision.
- Si l'usage admin devient en pratique plus continu que prévu (ex : utilisation simultanée comme tableau de bord temps réel), la décision peut être révisée en introduisant un refresh token admin à durée courte (par exemple 8 heures), à ce moment-là explicitement documentée dans un nouvel ADR.

## Implémentation

### Backend

Trois fichiers concernés :

- `backend/padel-manager/src/main/resources/application.yml` : ajout de la property `jwt.admin-expiration` (default `7200000` ms = 2h, surchargeable via `JWT_ADMIN_EXPIRATION`).
- `backend/padel-manager/src/main/java/be/ephec/padelmanager/service/impl/JwtService.java` : ajout du champ `adminExpiration` injecté via `@Value`, et de la méthode `generateAdminToken(String subject, String role, Integer idSite)` qui mirorre `generateToken` en utilisant `adminExpiration`.
- `backend/padel-manager/src/main/java/be/ephec/padelmanager/service/impl/AuthService.java` : la méthode `adminLogin()` appelle `jwtService.generateAdminToken(...)` au lieu de `jwtService.generateToken(...)`.

L'interface `IJwtService` a été étendue avec la signature `generateAdminToken` pour permettre l'injection via le type interface dans `AuthService`.

### Frontend

Aucune modification frontend n'est nécessaire pour cette décision. Le comportement actuel est déjà cohérent :

- `AuthService.adminLogin()` (`frontend/src/app/core/services/auth.service.ts`) appelle `hydrateFromToken(response.accessToken)` sans passer de refresh token, parce que la réponse backend n'en contient pas.
- À l'expiration du token, `hydrate()` détecte l'access token expiré et, en l'absence de refresh token, appelle `storage.clear()` puis redirige vers `/login`.

Ce comportement, observé lors du diagnostic Sprint 3A scénario F, est désormais documenté comme intentionnel.

## Références

- Audit initial : finding F-02 — "Admin login has no refresh-token path"
- Diagnostic : Sprint 3A Diagnosis Report, scénario F (observation only)
- Commit d'implémentation : `7c52827 feat(auth): extend admin access token TTL to 2h`
- OWASP ASVS v4.0.3, section 3.3 — Session Management : Session Timeout
