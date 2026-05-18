# ADR-0003 : Index unique partiel sur participation pour autoriser la réinscription après annulation

## Statut

Accepté — 2026-05-18

## Contexte

La table `participation` lie un membre (`matricule`) à un match (`id_match`) avec
un statut pouvant valoir `EN_ATTENTE`, `CONFIRME` ou `ANNULEE`. Depuis V1, la
contrainte `UNIQUE (id_match, matricule)` garantit qu'un membre n'apparaît qu'une
seule fois par match, indépendamment du statut de sa participation.

Cette contrainte s'avère trop stricte au regard des règles métier. Lorsqu'un
membre annule sa participation (statut → `ANNULEE`), le système doit pouvoir
l'autoriser à se réinscrire ultérieurement au même match si des places restent
disponibles. Or, la contrainte stricte bloque toute tentative d'insertion d'une
nouvelle ligne `(id_match, matricule)`, même quand la seule ligne existante pour
ce couple est annulée.

Ce problème a été identifié lors du Sprint T-A (diagnostic Section 4) comme un
bloquant pour UC-04 (inscription à un match public). Aucune logique service ne
peut contourner une contrainte d'unicité violée au niveau de la DB : un `INSERT`
sur une ligne annulée déclencherait systématiquement une `DataIntegrityViolation`.

## Décision

La contrainte `UNIQUE (id_match, matricule)` est remplacée par un index unique
partiel PostgreSQL excluant les lignes dont le statut est `'ANNULEE'` :

```sql
ALTER TABLE participation DROP CONSTRAINT participation_id_match_matricule_key;

CREATE UNIQUE INDEX participation_match_membre_active_uidx
    ON participation (id_match, matricule)
    WHERE statut <> 'ANNULEE';
```

L'unicité est désormais garantie uniquement pour les participations actives
(`EN_ATTENTE` ou `CONFIRME`). Plusieurs lignes `ANNULEE` peuvent coexister pour
le même `(id_match, matricule)` — elles constituent un journal d'annulations.

## Justification

- **Préserve l'historique des annulations** : les lignes `ANNULEE` ne sont pas
  supprimées ; elles constituent un journal d'audit consultable.
- **Débloque UC-04** : un membre ayant annulé peut se réinscrire sans modification
  de la couche service — la DB accepte le nouvel `INSERT`.
- **Aligne le comportement DB avec l'intention métier** : la règle est "un membre
  ne peut pas être inscrit activement deux fois au même match", non pas "un membre
  ne peut jamais avoir eu deux lignes pour le même match".
- **Cohérence avec le trigger existant** : `tr_participation_max_joueurs` (V1)
  compte déjà les joueurs actifs avec `WHERE statut != 'ANNULEE'` — le nouveau
  comportement reprend exactement la même exclusion.
- **Coût minimal** : un index partiel PostgreSQL natif, aucune extension requise,
  aucune logique service supplémentaire.

## Conséquences

### Positives

- Un membre peut se réinscrire à un match qu'il a précédemment annulé.
- L'historique complet des annulations est conservé pour audit et reporting.
- La contrainte d'unicité active reste garantie par la DB, sans déplacer la
  responsabilité vers le code applicatif.
- Compatible avec la sémantique du trigger `tr_participation_max_joueurs`, qui
  exclut déjà les lignes `ANNULEE` du comptage.

### Négatives / arbitrages assumés

- Un membre peut théoriquement annuler et se réinscrire N fois : N lignes
  `ANNULEE` s'accumulent en historique. À monitorer en production si un comportement
  abusif (oscillation inscription/annulation) est observé.
- L'unicité est portée par `pg_indexes` et non `pg_constraint`. Des outils
  d'inspection de schéma qui ne listent que les contraintes (certains ORMs,
  certains GUI DB) ne verront pas cet index — un développeur nouveau doit lire
  cet ADR pour comprendre la structure.
- En cas de migration de DB vers un SGBD ne supportant pas les index partiels
  (MySQL < 8.0, SQL Server avec limitations), cette décision devra être revisitée.

## Implémentation

- Migration V11 : `backend/padel-manager/src/main/resources/db/migration/V11__partial_unique_participation.sql`
- Commit : `<à renseigner après commit>`
- Tests d'intégration validant le comportement (réinscription après annulation) : Sprint T-B4 (à venir)

## Références

- Sprint T-A diagnostic, Section 4 — identification de la contrainte bloquante
- ADR-0002 : architecture scheduler et traitements automatiques des matchs
- PostgreSQL documentation : [Partial Indexes](https://www.postgresql.org/docs/current/indexes-partial.html)
- UC-04 : inscription match public (CDC Padel Manager EPHEC 2025-2026)
