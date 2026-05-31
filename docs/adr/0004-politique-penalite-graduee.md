# ADR-0004 : Politique de pénalité graduée — seule la création de match est bloquante

## Statut

Accepté — 2026-05-19

## Contexte

La version initiale du système appliquait les pénalités de manière uniforme : tout
membre ayant une pénalité active (`date_fin > NOW()`) se voyait refuser l'accès
à trois opérations distinctes :

1. **Créer un match** (privé ou public) — `verifierConditionsCreation`
2. **Être invité à un match privé** par l'organisateur — `ajouterJoueur`
3. **S'inscrire à un match public** de son propre chef — `sInscrireMatchPublic`

Cette approche "tout ou rien" a été jugée trop restrictive lors de la conception
du Sprint F-M4. Elle posait deux problèmes concrets :

- **UC-04 (inscription match public)** : un membre pénalisé pour n'avoir pas rempli
  son propre match ne devrait pas être empêché de compléter le match d'un autre,
  ce qui ne fait qu'aggraver la pénurie de joueurs.
- **UC-03 (invitation par l'organisateur)** : l'organisateur est déjà responsable
  de choisir ses joueurs ; lui laisser la décision d'inviter ou non un membre
  pénalisé relève de son jugement, pas d'une règle système.

## Décision

La vérification de pénalité active est **conservée uniquement dans
`verifierConditionsCreation`**, qui gouverne la création de match (UC-02 / UC-03).

Elle est **supprimée** de :
- `ajouterJoueur` (invitation organisateur, UC-03 section ajout)
- `sInscrireMatchPublic` (auto-inscription membre, UC-04)

En résumé : **un membre pénalisé ne peut pas créer de match, mais peut être
invité ou s'inscrire à un match existant**.

## Justification

- **Graduation du risque** : créer un match engage 60 € de responsabilité
  potentielle (4 places × 15 €) ; s'inscrire à un match existant n'engage que
  sa propre place. La règle est proportionnée à l'engagement financier.
- **Fluidité du remplissage** : les matchs publics incomplets ont besoin de joueurs.
  Bloquer les membres pénalisés sur les inscriptions aggrave le problème que les
  pénalités cherchent à résoudre (matchs qui ne se remplissent pas).
- **Responsabilité de l'organisateur** : l'organisateur d'un match privé est déjà
  responsable de la composition de son équipe. Le contraindre à écarter des membres
  pénalisés retire une partie de son autonomie sans bénéfice métier clair.
- **Cohérence avec le solde dû** : le blocage sur `soldeDu` (dette financière
  concrète) reste en place pour les trois opérations, car il représente un risque
  financier direct et immédiat.

## Conséquences

### Positives

- Moins de friction pour les membres pénalisés qui souhaitent participer à des
  matchs déjà planifiés par d'autres.
- Réduit le risque qu'un match pénalise deux fois l'organisateur (pénalité déjà
  reçue + match qui ne se remplit pas parce que les participants potentiels sont
  bloqués).
- Logique service simplifiée : deux blocs de code supprimés, responsabilité de
  `verifierConditionsCreation` mieux délimitée.

### Négatives

- Un membre pénalisé peut participer à des matchs en tant que joueur invité ou
  auto-inscrit. Certains pourraient considérer cela comme un contournement partiel
  de la sanction.
- Les tests qui validaient le comportement bloquant sur `ajouterJoueur` et
  `sInscrireMatchPublic` ont été adaptés pour vérifier l'inverse
  (pas d'appel à `penaliteRepo` dans ces chemins).

## Alternatives envisagées

- **Blocage total (statu quo)** : rejeté — trop restrictif et contre-productif
  pour le remplissage des matchs.
- **Blocage configurable par type de membre** : techniquement possible mais hors
  périmètre académique ; introduirait une complexité non justifiée au stade actuel.
- **Pénalité avec niveau de sévérité** : envisageable à terme (ex. : niveau 1 =
  création bloquée, niveau 2 = inscription aussi bloquée), mais nécessite un
  changement de schéma DB et un cycle de sprint dédié.
