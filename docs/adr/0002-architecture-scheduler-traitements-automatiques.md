# ADR-0002 — Architecture du scheduler : traitements automatiques

## Statut

Accepté — 2026-05-17

## Contexte

L'application Padel Manager doit implémenter trois traitements automatiques déclenchés par le temps (Sprint 2B), décrits dans le CDC aux sections CF-M-004 à CF-M-008 (p. 24-25) et dans la section « Traitements Automatiques » (p. 49) :

- **Job 1 — Bascule des matchs privés incomplets** : un match privé dont le créneau commence sans avoir atteint 4 joueurs confirmés est automatiquement basculé en match public.
- **Job 2 — Libération des places non payées** : les participations en statut `EN_ATTENTE` (non confirmées par paiement) dont le créneau a démarré sont libérées, avec pénalité potentielle sur le membre concerné.
- **Job 3 — Calcul du solde organisateur au démarrage du match** : lorsqu'un match passe à l'état `DEMARRE`, le `soldeDu` de l'organisateur est ajusté en fonction du nombre de joueurs confirmés présents.

Au moment de l'initialisation de Sprint 2A, le backend présentait les lacunes suivantes :

- `@EnableScheduling` absent de `PadelManagerApplication` — aucun job `@Scheduled` ne peut s'exécuter.
- Le statut `DEMARRE` inexistant — aucun marqueur d'idempotence disponible pour Job 3.
- Aucune contrainte CHECK sur `match_padel.statut`, `match_padel.type_match`, `participation.statut` — des valeurs invalides peuvent être insérées silencieusement.
- `MatchPadelRepo` et `ParticipationRepo` sans méthodes de recherche temporelle — les jobs ne peuvent pas filtrer les matchs par fenêtre horaire.

## Décision

**3.1** Spring `@EnableScheduling` + `@Scheduled` est adopté comme moteur d'ordonnancement.

**3.2** Un nouveau statut de cycle de vie `DEMARRE` est introduit sur `match_padel` comme marqueur d'idempotence pour Job 3.

**3.3** Les statuts et types de match sont représentés par des classes de constantes statiques Java (`MatchStatus`, `MatchType`, `ParticipationStatus`) plutôt que par des enums JPA `@Enumerated`.

## Justification

### 4.1 Spring @Scheduled vs alternatives (Quartz, événements asynchrones)

Spring `@Scheduled` est intégré nativement dans Spring Boot sans dépendance supplémentaire. Pour des jobs horaires simples sans état distribué, il est suffisant et préférable à Quartz, qui requiert une table de persistance des triggers (`QRTZ_*`), un `JobStore`, et une configuration significative en XML ou Bean.

À l'échelle d'une application académique mono-instance sans contrainte de reprise après crash, la complexité opérationnelle de Quartz n'est pas justifiée. La sémantique « au moins une fois » assurée par `@Scheduled` combinée au marqueur d'idempotence `DEMARRE` (voir 4.2) est suffisante pour les trois jobs.

Les événements asynchrones Spring (`ApplicationEventPublisher`) ont été écartés car les traitements sont déclenchés par le temps, non par des actions utilisateur.

### 4.2 Statut DEMARRE comme marqueur d'idempotence (vs colonne boolean)

Job 3 calcule le `soldeDu` de l'organisateur au moment du démarrage effectif d'un match. Sans marqueur, chaque exécution du scheduler retraiterait les mêmes matchs, entraînant une double-facturation.

Deux options ont été évaluées :

| Option | Avantages | Inconvénients |
|---|---|---|
| Colonne boolean `traitement_effectue` | Simple | Migration supplémentaire, aucune sémantique métier, invisible en administration |
| Statut `DEMARRE` | État métier observable, lisible en administration, compatible CHECK | Introduit un état de plus dans le cycle de vie du match |

Le statut `DEMARRE` est retenu parce qu'il encode simultanément l'état métier (le match a démarré) et la garantie d'idempotence dans la colonne `statut` déjà présente, sans migration de schéma supplémentaire au-delà du CHECK constraint de V10.

### 4.3 Constantes statiques vs enums JPA @Enumerated

L'approche idéale serait d'utiliser des enums Java avec `@Enumerated(EnumType.STRING)` sur les entités JPA, ce qui fournirait une validation à la compilation. Cette approche a été écartée dans ce sprint pour les raisons suivantes :

- Les services existants (`MatchPadelService`, `PaiementService`, `ParticipationService`) utilisent des String literals directes dans environ 25 occurrences réparties sur 5 fichiers.
- Migrer vers `@Enumerated` implique de modifier les entités `MatchPadel.java` et `Participation.java`, ainsi que tous leurs usages de service, ce qui dépasse le périmètre d'un sprint de fondations.
- Des classes de constantes statiques (`public static final String`) offrent une compatibilité immédiate : les services existants continuent de fonctionner sans modification, et les nouveaux jobs utilisent les constantes dès Sprint 2B.
- La migration progressive des literals existants vers les constantes peut s'effectuer en Sprint 2B sans risque de régression à la compilation.

**Cette décision représente de la dette technique assumée.** Les constantes statiques ne valident pas à la compilation et ne modifient pas le comportement JPA. Les String literals dans les services existants persistent jusqu'à leur remplacement explicite en Sprint 2B.

## Conséquences

### Positives

- `@EnableScheduling` activé : les trois jobs `@Scheduled` de Sprint 2B peuvent être déclarés immédiatement.
- Les contraintes CHECK V10 protègent la base de données contre les insertions de valeurs invalides sur `match_padel.statut`, `match_padel.type_match` et `participation.statut`.
- `MatchPadelRepo` et `ParticipationRepo` exposent les méthodes JPQL avec `JOIN FETCH`, évitant les `LazyInitializationException` dans les jobs hors contexte HTTP.
- `DEMARRE` est contraint en DB, disponible via `MatchStatus.DEMARRE`, et lisible dans les interfaces d'administration.

### Négatives / arbitrages assumés

- Le pool de threads du scheduler reste celui par défaut Spring (1 thread unique). Si deux jobs partagent la même planification et que le premier excède la période, le second est mis en file d'attente. À l'échelle académique ce comportement est acceptable ; une configuration `spring.task.scheduling.pool.size` peut être ajoutée si nécessaire.
- Les String literals dans les services existants coexistent avec les nouvelles constantes jusqu'à Sprint 2B. Pendant cette transition, une faute de frappe dans un service ne sera pas détectée à la compilation.
- Le statut `DEMARRE` est définitif — aucune interface ne permet de le réinitialiser manuellement (conforme au CDC : les traitements automatiques sont irréversibles).

## Implémentation

### Sprint 2A (ce sprint) — Fichiers créés ou modifiés

| Fichier | Action | Rôle |
|---|---|---|
| `src/main/resources/db/migration/V10__match_lifecycle_and_check_constraints.sql` | Créé | CHECK constraints sur statuts + indexes scheduler |
| `src/main/java/.../model/MatchStatus.java` | Créé | Constantes statiques statut `match_padel` |
| `src/main/java/.../model/MatchType.java` | Créé | Constantes statiques `type_match` |
| `src/main/java/.../model/ParticipationStatus.java` | Créé | Constantes statiques statut `participation` |
| `src/main/java/.../PadelManagerApplication.java` | Modifié | Ajout `@EnableScheduling` |
| `src/main/java/.../repository/MatchPadelRepo.java` | Modifié | 2 méthodes JPQL avec `JOIN FETCH` temporel |
| `src/main/java/.../repository/ParticipationRepo.java` | Modifié | 2 méthodes JPQL (temporelle + comptage par statut) |
| `docs/adr/0002-architecture-scheduler-traitements-automatiques.md` | Créé | Ce document |

Package de base : `be.ephec.padelmanager` (abrégé `...` dans le tableau).

### Sprint 2B (planifié) — Travaux de suite

- Implémenter les trois beans `@Scheduled` utilisant les nouvelles méthodes repo et les constantes de statut.
- Migrer progressivement les String literals des services existants vers `MatchStatus`, `MatchType`, `ParticipationStatus`.
- Valider l'application de V10 en environnement local après `docker compose down -v && docker compose up -d`.

## Références

- CDC sections CF-M-004 à CF-M-008 (p. 24-25) : règles métier détaillées des traitements automatiques (bascule matchs privés, libération places, solde organisateur).
- CDC section « Traitements Automatiques » (p. 49) : vue d'ensemble du module scheduler.
- Document source : `docs/cdc/CDC_GestionTerrainPadle.pdf`.
- ADR-0001 (`docs/adr/0001-sessions-admin-sans-refresh-token.md`) : référence de style et structure Nygard adoptée dans ce projet.
- Commit d'implémentation Sprint 2A : `<commit-hash-2A>` *(à remplir par Aimé après commit)*.
