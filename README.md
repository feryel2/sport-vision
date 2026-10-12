# Sport Vision — exercices, programmes et séances

Angular 17.3 / Spring Boot 3.2.5 / Java 17. Template et ports conservés.

## Lancement et URL

Dans `backend` : `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
Dans `frontend` : `npm ci` à la première installation, puis `npm start`.
Lancer Maven depuis `backend` : son cache est dans `.m2/repository`.
Alternative après compilation : `java -jar target/sport-vision-training-backend-1.0.0.jar --spring.profiles.active=local`.

- Accueil : http://127.0.0.1:4201/entrainement
- Catalogue : http://127.0.0.1:4201/entrainement/exercices
- Administration : http://127.0.0.1:4201/admin/entrainement/exercices
- Programmes : http://127.0.0.1:4201/entrainement/programmes
- Création : `/entrainement/programmes/nouveau` ; fiche : `/entrainement/programmes/:id` ; édition : `/:id/modifier`.
- Exécution / récapitulatif : `/entrainement/seances/:id`.
- Historique : http://127.0.0.1:4201/entrainement/historique
- Progression : http://127.0.0.1:4201/entrainement/progression
- API : http://127.0.0.1:8082/api/training ; URL Angular centralisée et proxy dédié.

## Données et démonstration

Le profil `local`, activé explicitement, conserve H2 dans
`backend/data/sportvision_training_local.mv.db` : ne pas supprimer ce fichier.
`ddl-auto=update` ajoute le schéma sans réinitialiser les données. `dev` reste en mémoire.
Les 12 exercices initiaux sont ajoutés une seule fois, via le marqueur
`training_demo_seeds`. Désactivation : `--training.demo.enabled=false`.
Aucun programme ni historique fictif n’est ajouté au démarrage.

`TrainingCurrentUserProvider` fournit le propriétaire côté serveur. En `local`,
tous les visiteurs partagent `training-local-demo`. Hors `local`, sans fournisseur réel,
les opérations privées renvoient 403. Une ressource d’un autre propriétaire renvoie 404.
**Ce mécanisme n’est pas une authentification. L’administration du catalogue n’est
pas protégée : ajouter authentification et autorisations avant tout déploiement public.**
Aucun lien avec la boutique, aucun fallback localStorage.

## Programmes et révisions

Un programme contient des séances ordonnées et leurs prescriptions. Chaque séance
comporte au moins un exercice : séries positives, répétitions OU durée selon la mesure,
repos et charge non négatifs. La charge est facultative, en kg.
Les prescriptions gardent un instantané du nom, matériel et type de mesure.
Pour conserver une prescription, utiliser son `existingPrescriptionId` courant.
Un exercice archivé déjà prescrit reste conservable mais ne peut pas être ajouté.
La duplication crée des identifiants indépendants.

Chaque modification effective, archivage ou réactivation crée une révision immuable,
atomiquement. Un enregistrement identique ne crée pas de révision.
`expectedRevision` et un verrou refusent les modifications périmées (409).
Un programme archivé reste consultable ; le réactiver avant édition.
L’éditeur avertit avant de perdre des saisies et les conserve après conflit.

## Réalisation et historique

« Démarrer cette séance » copie programme, révision, séance, informations utiles des
exercices et prescriptions. Les changements ultérieurs ne modifient pas cet instantané.
Une contrainte unique en base garantit une séance en cours par propriétaire :
un démarrage identique la reprend ; un autre renvoie 409 avec sa référence.
Les séries démarrent en attente, sans performances préremplies. Valider ou corriger
enregistre les valeurs réelles ; ignorer efface la performance de la série.
Quitter ne l’abandonne pas. Les saisies non enregistrées déclenchent un avertissement.

Terminer exige au moins une série réalisée. Les séries encore en attente nécessitent
une confirmation explicite du serveur (409), puis deviennent ignorées atomiquement.
Abandonner conserve les résultats enregistrés. Toute séance close devient non modifiable ;
répéter sa même finalisation est sans effet. Le commentaire facultatif est enregistré
à la clôture. Le temps écoulé vient des dates serveur, repos compris ; aucune calorie.
L’historique paginé et filtré utilise les instantanés et présente le prévu / réalisé.

## Tableau de progression

GET `/progress?days=30` (7, 30 ou 90 jours) renvoie indicateurs, journées et séries
par exercice. GET `/progress/exercises/{id}?days=30&measurementType=REPETITIONS|DURATION`
renvoie les points sélectionnés. Propriétaire exclusivement fourni côté serveur.
Tous les calculs métier sont centralisés dans `TrainingProgressService`.

Période : aujourd’hui et les N−1 jours précédents dans `Africa/Tunis`, depuis minuit
du premier jour inclus jusqu’à l’instant de consultation exclu ; aujourd’hui est partiel.
Les bornes et dates de séance restent des instants UTC. Rattachement par date de fin,
regroupement quotidien dans Africa/Tunis. Les jours sans séance terminée valent zéro.
Indicateurs : séances COMPLETED ; ABANDONED séparément ; séries DONE des COMPLETED ;
somme des durées début–fin des COMPLETED, en secondes entières, **repos compris**.

Évolution : uniquement les séries DONE des séances COMPLETED de la période. Un point
par séance et couple identifiant source d’exercice/type de mesure, même si l’exercice
est archivé. Les occurrences répétées de ce couple dans la séance sont réunies.
Répétitions : somme des répétitions ; durée : somme des secondes d’effort (hors repos) ;
charge : maximum renseigné, null si absent, zéro si effectivement saisi.
Libellés historiques conservés par point ; aucun calcul depuis le catalogue actuel.
Les séries SKIPPED et séances en cours/abandonnées ne contribuent pas aux performances.
Aucun score, taux d’assiduité ou interprétation automatique d’amélioration.
SVG locaux accessibles et tableaux de valeurs ; aucun ajout de dépendance graphique.

Vérification progression (05/10/2026) : compilations Angular/Spring Boot réussies,
un seul nouveau test d’agrégation sur H2 isolée (propriétaires, statuts, séries,
bornes Tunis, instantanés, types distincts, charge absente/zéro). Les 19 tests existants
ont aussi été exécutés, car la configuration de sécurité commune a été étendue :
20 tests sans échec. Périodes 7/30/90 et historique à un point vérifiés dans le navigateur ;
état vide vérifié avec une base temporaire en mémoire, sans ajout à la base locale.
## API et limites

Réponses `{success,message,data}` ; validation 400, identité absente 403,
ressource absente/non possédée 404, conflit 409, sans détails internes.
- `/exercises` : catalogue, options, filtres, tri autorisé et archivage réversible.
- `/programs` : GET (search, status=ACTIVE|ARCHIVED|ALL, page, size), POST.
- `/programs/{id}` : GET, PUT (`{expectedRevision,program}`).
- POST `/programs/{id}/duplicate` ; PATCH `/{id}/archive|restore` (`{expectedRevision}`).
- GET `/programs/{id}/revisions` (page, size), `/revisions/{revision}`.
- POST `/workouts` : `{programId,revision,sessionId}` ; GET `/current`, GET `/{id}`.
- PUT `/workouts/{id}/sets/{setId}` : `{repetitions,durationSeconds,loadKg}`.
- PATCH `/workouts/{id}/sets/{setId}/skip`.
- POST `/workouts/{id}/complete` : `{confirmSkipPending,comment}` ; POST `/{id}/abandon` : `{comment}`.
- GET `/workouts` : status=IN_PROGRESS|COMPLETED|ABANDONED facultatif, page, size.
Tri stable : programmes par nom/id, révisions décroissantes, séances par début/id décroissants.

Plafonds techniques, sans valeur de recommandation sportive : programmes JSON 1 Mio,
20 séances, 30 prescriptions/séance ; noms 120 caractères, descriptions/commentaires
2 000, objectif 500 ; séries 1–100, répétitions 1–10 000, durée 1–86 400 s,
repos 0–86 400 s, charge 0–10 000 kg (2 décimales). Pagination : taille 1–50,
page 0–100 000 ; recherche 120 caractères.

## Vérification

`backend` : `mvn test` / `mvn package`. `frontend` : `npm run build`.
19 tests passent sur H2 éphémères séparées : 5 catalogue, 10 programmes, 1 identité
absente et exactement 3 nouveaux tests séances (instantané, isolation, clôture idempotente).
Les tests existants ont été relancés car la sécurité et le traitement des erreurs sont communs.
Compilations réussies le 05/10/2026. Parcours navigateur réel : démarrer, saisir,
recharger, reprendre, confirmer la fin, consulter/filtrer l’historique, sortie protégée
et abandon conservant les résultats. Six démarrages HTTP concurrents donnent une seule
séance ; autre séance refusée par 409. Historique contrôlé à 390 px.

À cette étape historique : calendrier, remplacement d’exercice et adaptation IA étaient hors périmètre.

Après arrêt/redémarrage local, les deux séances de vérification (terminée et abandonnée)
ont été comparées intégralement : identiques, sans séance encore active. Les 14 exercices
et les 2 programmes préexistants restent inchangés. Ces deux essais réels sont conservés.


Contrôles complémentaires : nouvelle tentative après erreur réseau, lien du tableau
vers la séance et adaptation mobile à 390 px vérifiés. Le serveur utilise de nouveau
la base locale. Comparaison avant/après : 2 séances, 14 exercices et 2 programmes
inchangés ; aucune donnée utilisateur ajoutée pour cette étape.

## Génération IA d’un programme

La confirmation POST `/api/training/ai/programs` exige désormais un champ
`idempotencyKey` (UUID), en plus de `preferences` et `proposal`. Angular crée cette
clé à réception de chaque nouvelle proposition et la conserve lors des nouvelles
tentatives d'enregistrement. L'aperçu et sa clé restent en mémoire dans la page ;
ils ne sont pas restaurés après rechargement du navigateur.

La table `training_ai_confirmations` conserve le propriétaire serveur, la clé,
l'empreinte du contenu et le programme associé. Programme, révision initiale et
confirmation sont enregistrés dans une seule transaction. Une contrainte unique
sur propriétaire/clé empêche les créations concurrentes de produire des doublons
validés en base. Une répétition renvoie l'état courant du même programme, sans
nouvelle révision ; un contenu différent avec la même clé est refusé par HTTP 409.
Une nouvelle clé permet une nouvelle création volontaire. Le format de réponse
et le statut HTTP 201 existants sont conservés, y compris lors d'une répétition.
Les anciens clients sans clé reçoivent HTTP 400. En profil local, la nouvelle table
sera ajoutée par `ddl-auto=update` au prochain démarrage ; ne pas réinitialiser H2.

Page : http://127.0.0.1:4201/entrainement/programmes/generer (bouton dans Mes programmes).
Gemini 2.5 Flash configurable, appel HTTP direct côté serveur, aperçu validé sans écriture,
puis confirmation via le service métier. Aucun historique transmis, aucune charge prescrite.
Créer soi-même `backend/.env.local` selon le guide [IA-SIMPLE](docs/IA-SIMPLE.md), puis
lancer `backend/start-local.ps1` après compilation. Ne pas partager la clé.
Guide court, fichiers, exemples et commandes : [IA-SIMPLE](docs/IA-SIMPLE.md).
Sans clé : métier opérationnel et « Service IA non configuré ».
Tests de génération avec Gemini simulé : `mvn "-Dtest=TrainingAiApiTest" test`.
Schéma fournisseur simplifié, contrôles Java conservés : appel réel Gemini HTTP 200 le 05/10/2026,
aperçu validé puis programme 33 enregistré une seule fois (révision 1), programmes existants préservés.
Lors du diagnostic historique du 05/10/2026 : backend compilé et cinq tests IA ciblés
réussis, sans suite complète à cette étape (voir docs/IA-SIMPLE.md).

## Bilan et adaptation IA

Disponible depuis la fiche d'un programme actif : bilan calculé en Java, proposition
Gemini limitée aux prescriptions existantes, aperçu avant/après, confirmation serveur
avec nouvelle révision. Aucun changement automatique ni historique de séance modifié.
Voir [fonctionnement, limites et endpoints](docs/ADAPTATION-IA.md).

## Planification des séances

Page : `http://127.0.0.1:4201/entrainement/planification`, accessible depuis la navigation
ou « Planifier cette séance » dans un programme actif. Choisir une séance et une date/heure
future à la minute, explicitement dans `Africa/Tunis`. Java convertit en instant UTC ;
l'interface affiche l'heure de Tunis indépendamment du fuseau de l'appareil.

La table `training_schedules` conserve le propriétaire serveur, le programme, la révision
et la séance sélectionnés. Une modification ultérieure du programme ne change pas la
planification : le démarrage utilise la révision conservée. Un programme archivé bloque
création, changement de date et nouveau démarrage ; l'annulation reste possible.
Les dates peuvent être modifiées tant que l'entraînement n'a pas commencé. L'annulation
conserve le créneau dans la liste et libère son horaire. Aucun historique n'est supprimé.

API `/api/training/schedules` : `GET ?from=YYYY-MM-DD&to=YYYY-MM-DD&page=0&size=20`
(dates inclusives, période maximale 93 jours), `POST` avec `programId`, `expectedRevision`,
`sessionId`, `scheduledLocal` (ISO local sans décalage, par exemple `2030-01-02T18:30`),
`PUT /{id}` avec `expectedVersion`, `scheduledLocal`, et `POST /{id}/cancel` ou `/start`
avec `expectedVersion`. Une version périmée ou un créneau occupé renvoie 409.

Une contrainte unique empêche deux créneaux non annulés au même instant pour un propriétaire.
Le démarrage verrouille la planification et réutilise `TrainingWorkoutService` dans la même
transaction : une requête répétée retrouve le même entraînement, même clôturé. Une séance
manuelle déjà en cours et identique peut être reprise ; elle ne peut être liée qu'à une seule
planification. Une autre séance active produit un conflit. Démarrer en avance ou en retard
est autorisé. Pas de détection de chevauchement par durée, de calendrier externe ni de notification.

Les états affichés sont planifiée, en cours, effectuée, annulée et abandonnée. L'état
d'exécution est lu directement sur l'entraînement existant, qui reste dans l'historique
habituel. Une séance abandonnée n'est pas redémarrée : créer un nouveau créneau si nécessaire.
Au prochain démarrage local, `ddl-auto=update` ajoutera la table sans réinitialiser H2.
Tests isolés : `mvn --offline "-Dtest=TrainingScheduleApiTest" test` ; aucune utilisation de Gemini.

Vérification de cette étape : 11 tests de planification réussis ; suite backend complète,
56 tests réussis, aucun échec. Build Angular de production réussi. Aucun test navigateur
effectué et aucune modification de la base locale pendant ces vérifications.

