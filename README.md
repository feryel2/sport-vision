# Sport Vision — Boutique et Entraînement

Application universitaire Angular17 / Spring Boot3.2.5 / Java17 regroupant la Boutique de Feryel et le module Entraînement de Nour.

## Démarrage normal après clonage

Prérequis : Java17, Maven3.9, Node20 (au moins20.9), npm10. Angular17 n'est pas officiellement compatible avec Node22. Utiliser Node20 pour reproduire ce projet ancien.

Dans un premier terminal, depuis le dossier du dépôt :

```powershell
cd backend
mvn spring-boot:run
```

Dans un second terminal :

```powershell
cd frontend
npm ci
npm start
```

Ouvrir http://127.0.0.1:4200/entrainement. Backend : http://127.0.0.1:8080. Le proxy Angular conserve les chemins /api et cible8080. Aucun script PowerShell, outil de peuplement, fichier .env ni base H2 à copier n'est nécessaire.

Si8080 est occupé, utiliser une surcharge temporaire du port Spring et un proxy de développement correspondant. Ne pas arrêter un service existant pour libérer ce port. L'installation nécessite l'accès aux dépôts Maven/npm si les dépendances ne sont pas déjà en cache.

## Données locales et premier lancement

Sans profil explicitement choisi, Spring active `local-demo`. Ce profil est strictement lié à127.0.0.1 et utilise une nouvelle base persistante : `backend/data/local-demo/sportvision.mv.db` (chemin relatif au dossier backend).

Hibernate crée le schéma avec `ddl-auto=update`, jamais `create-drop`. Une transaction unique prépare80exercices,12programmes,24révisions,90séances terminées,1211séries (1205réalisées et6ignorées) et20planifications. Les90jours d'historique et les dates futures sont calculés au premier démarrage dans Africa/Tunis. Une séance synthétique du jour ne finit jamais dans le futur, même si le premier lancement a lieu tôt le matin.

Un marqueur durable empêche toute duplication. Aux lancements suivants, les enregistrements et modifications utilisateur sont conservés ; les dates ne sont pas décalées automatiquement. Si des données Training existent déjà sans marqueur, le peuplement est ignoré pour préserver ces données. Le nombre de lignes peut naturellement évoluer avec l'usage.

Le chargeur local est `TrainingLocalDatasetInitializer`; son catalogue et ses programmes portables sont dans `backend/src/main/resources/training/demo/`. Il réutilise les services métier, valide les DTO et affecte des dates historiques seulement aux nouveaux enregistrements synthétiques, dans la même transaction. Il ne fait aucun appel Gemini.

Les anciennes bases `data/integration-populated/` et `data/sportvision_training_local` ne sont pas utilisées par ce démarrage. Les fichiers de base, sauvegardes, secrets, node_modules, target et dist restent exclus de Git. Ne pas supprimer une base existante pour la repeupler.

## Fonctionnalités et architecture conservées

- Boutique : accueil, produits/catégories/marques/fournisseurs, panier, commandes, favoris, avis, nutrition, recommandations/coachs et backoffice.
- Entraînement : exercices, programmes versionnés, séances et historique, graphiques7/30/90jours, planification et trois parcours IA.
- Angular standalone : `frontend/src/app/features/front-office`, `back-office`, `training`, layouts partagés et services `/api`.
- Backend unique : `com.sportvision.SportVisionApplication`; domaine Training isolé sous `com.sportvision.training`, tables `training_`.

Les fallbacks/localStorage Boutique restent présents. Une liste de démonstration ne prouve pas une écriture serveur. Les défauts hérités du formulaire catégorie, de la confirmation locale de produit et du style de l'accueil ne sont pas corrigés par ce changement de démarrage.

## Profils et sécurité

- `local-demo` : défaut uniquement si aucun profil explicite ; H2 persistante dédiée et peuplement automatique unique.
- `integration` : ancienne démonstration H2 en mémoire, activation explicite ; pas de nouveau peuplement automatique.
- `test` : H2 éphémère et Gemini simulé ; aucun chargeur local activé sauf tests dédiés.
- `local` / `dev` : anciens profils conservés, sans changement de leurs bases ; ne pas les choisir pour le nouveau démarrage normal.
- `mysql` : activation explicite, variables `SPORTVISION_MYSQL_URL`, `SPORTVISION_MYSQL_USERNAME`, `SPORTVISION_MYSQL_PASSWORD`, schéma préparé séparément et validation JPA.
- Tout profil explicitement nonlocal conserve le refus des API de démonstration. Exemple de sélection explicite : `mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"`.

Le mode local n'est pas une authentification de production. Le rôle Angular est une démonstration et le propriétaire Training est partagé côté serveur. Le profil local doit rester sur boucle locale ; ne pas l'exposer publiquement.

## Gemini : facultatif au démarrage

Aucune clé n'est requise pour utiliser le métier et consulter les bilans calculés en Java. Les trois fonctions Gemini restent disponibles après configuration serveur de `GEMINI_API_KEY`, éventuellement `GEMINI_MODEL` (défaut gemini-2.5-flash) et `GEMINI_TIMEOUT_SECONDS` (défaut45).

Fournir ces variables dans l'environnement du processus Spring Boot ou dans une configuration IDE locale non suivie. Maven/Spring ne charge pas automatiquement `.env.local`. Ne jamais mettre une clé dans Angular, un fichier suivi ou les arguments de commande. Sans clé, une demande Gemini valide indique que le service n'est pas configuré. Aucun appel réel n'est effectué automatiquement au démarrage.

Les données initiales contiennent24exercices débutants sans matériel, des alternatives compatibles et un historique éligible au bilan/adaptation pour les12programmes. Les réponses restent validées côté Java et les changements de programme nécessitent une confirmation utilisateur.

## Vérification et limites

Le travail d'intégration reste non committé. La vérification d'un nouvel environnement utilise donc un clone local neuf accompagné d'une copie exacte des sources non committées à examiner ; aucune base, dépendance installée ou clé n'est copiée. Un clone GitHub de la branche ne pourra recevoir ces changements qu'après leur commit et publication autorisés.

Les contrôles portent sur le premier démarrage, les comptes80/12/24/90/1211/20, le redémarrage sans duplication, la conservation des modifications et la sécurité des profils nonlocaux. Les tests backend utilisent uniquement H2 éphémère et aucun appel réel àGemini. Les commandes de validation normales sont `mvn test` depuis backend et `npm run build` depuis frontend.

Les scripts historiques et les outils `tools/integration-data/` restent facultatifs pour les anciens environnements ; ils ne font pas partie du démarrage normal décrit ci-dessus.

## Routes principales

- Boutique : `/`, `/catalogue`, `/panier`, `/favoris`, `/coach-matching`, `/aliments`, `/contact`, `/login`.
- Backoffice (garde admin démo conservée) : `/admin`, `/admin/produits`, `/admin/categories`, `/admin/commandes`, `/admin/aliments`, `/admin/utilisateurs`; `/admin/parametres` redirige vers `/admin`.
- Entraînement : `/entrainement`, `/entrainement/exercices`, `/entrainement/exercices/:id`.
- Programmes : `/entrainement/programmes`, `/generer`, `/nouveau`, `/:id`, `/:id/modifier`, `/:id/adaptation` sous ce préfixe.
- Séances : `/entrainement/seances/:id`, `/entrainement/historique`, `/entrainement/progression`, `/entrainement/planification`.
- Gestion des exercices : `/admin/entrainement/exercices`; `/admin/entrainement` y redirige.
- Alternatives IA : dialogue depuis la fiche programme. Les gardes de sortie des formulaires/programmes/séances restent présents.

## API principales

Aucun contexte servlet global n'est ajouté. Les préfixes restent compatibles :

| Domaine | Préfixes / exemples |
|---|---|
| Boutique | `/api/products`, `/api/categories`, `/api/brands`, `/api/suppliers`, `/api/orders`, `/api/favorites`, `/api/reviews`, `/api/foods` |
| Comptes et statistiques | `/api/auth/login`, `/api/auth/register`, `/api/auth/users`, `/api/stats/dashboard` |
| Recommandations/coachs existants | `/api/ai` |
| Entraînement | `/api/training/exercises`, `/api/training/programs`, `/api/training/workouts`, `/api/training/progress`, `/api/training/schedules` |
| IA entraînement | `/api/training/ai/generate`, `/api/training/ai/programs`, `/api/training/ai/programs/{id}/alternative`, `/replacement`, `/adaptation` sous le même programme |

La boutique conserve notamment les listes/CRUD de produits et catégories, produits vedettes/faible stock, création des commandes et `PATCH /api/orders/{id}/status`.
Les DTO des deux modules restent séparés. Les réponses entraînement utilisent `{success,message,data}` : validation 400, identité indisponible 403, ressource absente/non possédée 404, conflit 409.

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

## Détails API entraînement et limites

Les chemins courts ci-dessous sont relatifs à `/api/training`.

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

## IA et planification : contrats conservés

La génération IA passe par un aperçu validé avant sauvegarde. POST `/api/training/ai/programs` exige `preferences`, `proposal` et `idempotencyKey` UUID. Angular conserve la clé pour les répétitions d'une même proposition. Confirmation et programme sont enregistrés dans une transaction avec unicité propriétaire/clé ; une répétition renvoie le même programme, un contenu différent renvoie 409. Une nouvelle clé permet une nouvelle création. L'aperçu n'est pas restauré après rechargement.

Les alternatives sont sélectionnées dans le catalogue autorisé, sans transmettre l'objectif libre. Le bilan est calculé en Java sur les séances terminées, puis Gemini propose des adaptations limitées. Java valide, l'utilisateur confirme avec la révision attendue, et l'historique reste inchangé.

La planification conserve programme, révision et séance. Les dates sont saisies à la minute dans `Africa/Tunis`, stockées comme instants et affichées dans ce fuseau. `GET /api/training/schedules` accepte `from`, `to`, `page`, `size` (période maximale 93 jours). La création prend `programId`, `expectedRevision`, `sessionId`, `scheduledLocal`; les modifications et actions `/cancel`, `/start` prennent `expectedVersion`. Les conflits de version/créneau renvoient 409.
Le démarrage réutilise le système de séances existant ; les répétitions retrouvent le même entraînement. L'annulation ne supprime pas l'historique. Les états planifiée, en cours, effectuée, annulée et abandonnée sont conservés. Pas de calendrier externe, de notifications ni de détection de chevauchement par durée.

Guides conservés : [IA-SIMPLE](docs/IA-SIMPLE.md) et [ADAPTATION-IA](docs/ADAPTATION-IA.md). Ils décrivent aussi le module autonome et ses essais historiques : pour les ports, profils et commandes de l'intégration, ce README prévaut. Les résultats historiques ne prouvent pas le fonctionnement de l'application fusionnée.

## Résultats de vérification du démarrage normal

- Clone local neuf + sources non committées, sans base H2, .env.local ou node_modules copiés.
- Packaging Maven et installation npm ci réussis avec Java17 / Node20.20.2.
- Suite backend :62tests réussis,0échec,0erreur,0ignoré ; H2 éphémère et aucun appel Gemini réel.
- Build Angular production réussi. Avertissement hérité : catalogue CSS10,06kB pour un budget10kB (65octets de dépassement).
- Lancement Maven normal sans profil explicite : local-demo activé et base dédiée automatiquement peuplée.
- Vérification des comptes80/12/24/90/1211/20 et des prérequis IA.
- Redémarrage vérifié sans duplication ni perte d'une modification utilisateur de démonstration.

Les ports temporaires8084/4204 ont été utilisés sur le poste de vérification parce que8080 est occupé et les instances existantes doivent rester intactes. Le proxy temporaire de vérification adapte l'origine du port4204 à l'origine locale4200 autorisée ; il ne fait pas partie de la configuration livrée. Le démarrage normal utilise8080/4200.

Les résultats ne valident ni une authentification de production ni une requête Gemini réelle. Les défauts Boutique hérités et les avertissements d'espaces de l'ancien index ne sont pas corrigés ici. Ne pas finaliser la fusion avant revue et approbation.


## Training : démonstration automatique et copies privées (modifications locales)

Cette préparation est incluse dans les sources Java et les ressources `backend/src/main/resources/training/demo/`. Aucun fichier H2, script local, identifiant personnel ou secret Gemini n'est nécessaire. Les fichiers nouveaux doivent être inclus dans le futur commit après revue ; GitHub n'a pas été modifié.

Utiliser explicitement le profil H2 local, depuis `backend`, avec Java 17 et Maven :

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

Le profil lie le serveur à `127.0.0.1:8080`, utilise H2 persistante `./data/local-demo/sportvision` et le dialecte H2. Le premier démarrage prépare 80 exercices partagés, 12 programmes, 24 révisions, 90 séances terminées, 1 211 séries et 20 planifications. Les bases existantes ne sont pas réinitialisées. L'initialiseur existant conserve son marqueur `integration-populated-v1` et ignore les données Training non marquées déjà présentes.

Après inscription/connexion normale, ouvrir **Entraînement → Programmes** (ou l'historique/progression/planification). Lors du premier GET Training personnel approprié, le domaine Training copie le modèle partagé pour le propriétaire JWT `training-user:<id>`. Aucune modification du code d'inscription ou de JWT n'est nécessaire. Les 80 exercices restent communs ; chaque propriétaire reçoit ses propres 12 programmes, 24 révisions, 90 historiques, 1 211 séries et 20 planifications. Les identifiants internes et les références des instantanés sont remappés ensemble.

Une transaction et un marqueur durable `personal-demo-v1:<propriétaire>` empêchent les doublons ; le verrou du marqueur source protège les premiers accès concurrents. Les programmes, résultats et dates personnels existants ne sont jamais mis à jour. Si un créneau personnel est occupé, la nouvelle planification de démonstration est décalée à la minute suivante disponible. Les dates des nouvelles copies sont rapprochées de leur premier accès pour alimenter les graphiques 7/30/90 jours ; elles ne sont pas déplacées aux accès ou redémarrages suivants. Le modèle partagé n'est pas modifié par cette copie.

Scénarios synthétiques conseillés en révision 2 : **Full Body Débutant** (cibles atteintes, hausse prudente possible) et **Force fondamentale** (difficultés répétées, réduction/repos possibles). Java exige au moins deux séances comparables avec au moins deux séries réalisées par prescription ; il ne considère que la révision courante, parmi les 20 dernières séances terminées au maximum. Une nouvelle révision après adaptation n'aura pas automatiquement d'historique comparable : réaliser de nouvelles séances avant un autre bilan. Les anciennes séances restent intactes.

Les ajustements Gemini sont testés avec un client simulé. Le bilan Java et l'éligibilité sont disponibles sans appel IA ; la génération réelle de la proposition nécessite une clé serveur configurée séparément et une autorisation explicite pour les essais. Aucune clé ne figure dans les données ou dans Angular.

L'initialisation partagée et personnelle est exclue si `mysql` ou `production` est actif, même si `local-demo` est également sélectionné. Ne pas utiliser ces démonstrations en production. Les comptes ne sont pas créés par les données Training ; s'inscrire avec l'interface existante. L'administration conserve les règles de rôle existantes.

Une ancienne base déjà marquée n'est pas réécrite pour imposer les nouveaux exemples : ses enregistrements sont préservés et les nouvelles copies reflètent son modèle. Une base sans modèle partagé reconnu n'est pas complétée arbitrairement ; vérifier les journaux et utiliser un nouvel environnement H2 dédié après accord, jamais effacer une base existante.

La copie reconnaît les programmes du modèle par le nom de leur première révision comparé à la ressource portable. Les programmes partagés sans correspondance ne sont pas diffusés aux autres comptes. Un modèle incomplet ou ambigu est refusé avant toute copie ; aucune réparation destructive automatique n’est effectuée.
