# Première IA : proposer un programme

## Remplacer un exercice du programme

**Java filtre → Gemini choisit et explique → Java vérifie → le sportif confirme → nouvelle révision.**
Sur la version courante d’un programme actif, « Proposer une alternative » demande un motif
structuré, le matériel disponible et le niveau. Java vérifie le propriétaire et la prescription,
puis retient au maximum 40 exercices actifs : autre identifiant, même groupe musculaire principal
du catalogue, même mesure que la prescription, matériel et niveau compatibles, absents de la séance.
Sans candidat, message explicite et aucun appel Gemini. Aucun filtre n’est assoupli.
Le client HTTP et le modèle configurés sont réutilisés ; le prompt `alternative-prompt.txt`
demande uniquement `exerciseId` et `reason` (600 caractères maximum). Java vérifie la sélection.
L’aperçu n’écrit rien. L’annulation ferme la fenêtre sans modification.
POST `/api/training/ai/programs/{id}/alternative` prépare la proposition ; POST
`/api/training/ai/programs/{id}/replacement` reçoit la révision attendue, la prescription,
les préférences structurées et l’identifiant choisi, puis recontrôle tous les critères.
La confirmation verrouille le programme avant lecture et crée une seule révision via le mécanisme
existant ; une révision périmée retourne 409. Séries, répétitions/durée et repos sont conservés,
charge cible effacée ; les autres prescriptions gardent leurs instantanés. Les anciennes révisions
et les séances déjà démarrées ou terminées sont inchangées. Aucun remplacement depuis l’historique.
Deux tests isolés : `mvn "-Dtest=TrainingAlternativeApiTest" test` (sélection non autorisée,
confirmation versionnée, instantané antérieur préservé et confirmation périmée refusée).
Vérifié le 05/10/2026 : compilations Angular/backend et ces deux tests réussis. Un seul appel
réel Gemini HTTP 200 : Squats → Fentes alternées dans le programme 33. Aperçu sans écriture,
puis confirmation unique : révision 2, 3 séries de 12 répétitions, repos 60 s, charge vide.
Révision 1 conservée intégralement ; programmes 1 et 2 inchangés. Aucun exercice du catalogue modifié.

## Fichiers et parcours

- `TrainingAiController.java` : options, génération et confirmation HTTP sous `/api/training/ai`.
- `TrainingAiService.java` : filtre le catalogue, prépare le prompt, contrôle le JSON,
  puis appelle `TrainingProgramService.create` seulement à la confirmation.
- `GeminiClient.java` : un appel HTTP Java 17, délai maximal, lecture du texte et erreurs publiques.
- `TrainingAiDto.java` : préférences, proposition, aperçu et validation des champs.
- `TrainingAiException.java` : erreurs contrôlées, sans réponse brute ni secret.
- `resources/training/ai/program-prompt.txt` : consignes lisibles en français.
- `resources/training/ai/program-schema.json` : forme de la sortie JSON structurée.
- Angular : `ai-program.component.*`, `training-ai.service.ts`, `models/training-ai.ts`.
- `backend/start-local.ps1` : lit les variables locales comme du texte, sans les afficher.

Parcours : **formulaire → préférences + catalogue → prompt → HTTP Gemini → JSON →
validation serveur → aperçu → confirmation → nouvelle validation → création / révision 1**.
La génération n’écrit rien en base. Les noms d’exercices affichés viennent du catalogue
serveur ; les explications sont du texte interpolé, jamais du HTML interprété.

## Règles simples

L’objectif métier est actuellement du texte libre. Ici, quatre choix fixes (forme générale,
renforcement, endurance, mobilité) fournissent ce texte ; les programmes existants ne changent pas.
Niveau : débutant accepte BEGINNER ; intermédiaire ajoute INTERMEDIATE ; avancé accepte les trois.
Matériel : équipements cochés + NONE toujours disponible. Catalogue actif uniquement,
40 exercices maximum, triés par identifiant. Au moins 3 exercices compatibles sinon refus
avant appel Gemini. Chaque séance proposée contient 3 à 6 exercices distincts, réutilisables
dans d’autres séances, et il faut exactement 1 à 5 séances selon le choix.

Les bornes de `ProgramInput` sont réutilisées : séries 1–100, répétitions 1–10 000,
durée 1–86 400 s, repos 0–86 400 s. Ce sont des plafonds techniques, pas des conseils sportifs.
Répétitions OU durée selon le catalogue ; charge cible toujours null.
Les réponses inconnues, incomplètes, contradictoires ou hors limites sont refusées,
sans réparation, sans faux programme et sans écriture partielle.
L’enregistrement recontrôle catalogue actif, matériel, niveau, mesure et propriétaire serveur.

## Gemini vérifié

Modèle configurable retenu : `gemini-2.5-flash`, stable et listé dans l’offre gratuite
au 05/10/2026, sous réserve des quotas/disponibilités du compte.
POST `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`,
clé dans l’en-tête `x-goog-api-key`, corps `systemInstruction`, `contents`,
`generationConfig.responseMimeType: application/json` et `generationConfig.responseJsonSchema`.
Ce dernier est un objet JSON, jamais une chaîne ; aucun autre paramètre de schéma n’est envoyé.
Le schéma fournisseur se limite à type, properties, required et items. Les bornes, tailles,
valeurs d’énumération et contraintes de catalogue restent intégralement vérifiées en Java
et précisées dans le prompt. La mesure non applicable est omise ; tous les champs métier
obligatoires restent requis par Bean Validation, avec cohérence répétitions/durée contrôlée.
Un candidat, 8 192 tokens de sortie maximum, aucun outil ni retry automatique.
Seule une réponse complète STOP est acceptée ; blocage, troncature, réponse vide et
JSON invalide sont distingués. Le délai inclut la réception HTTP complète.
Sources officielles :
[modèle](https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash),
[offre gratuite](https://ai.google.dev/gemini-api/docs/pricing#gemini-2.5-flash),
[JSON structuré / REST](https://ai.google.dev/gemini-api/docs/generate-content/structured-output?hl=en).

## Exemple fictif, jamais ajouté automatiquement

Demande POST `/ai/generate` :
```json
{"objective":"GENERAL_FITNESS","level":"BEGINNER","sessionCount":1,"equipment":["NONE"]}
```

Exemple de texte JSON produit par Gemini, si les trois identifiants sont autorisés :
```json
{
  "name":"Découverte au poids du corps",
  "description":"Une séance variée à relire avant utilisation.",
  "justification":"Les trois exercices ne nécessitent pas de matériel.",
  "sessions":[{"name":"Découverte","exercises":[
    {"exerciseId":1,"measurementType":"REPETITIONS","sets":2,"repetitions":8,"restSeconds":45},
    {"exerciseId":2,"measurementType":"REPETITIONS","sets":2,"repetitions":10,"restSeconds":45},
    {"exerciseId":3,"measurementType":"DURATION","sets":2,"durationSeconds":20,"restSeconds":30}
  ]}]
}
```

L’API enveloppe cet objet dans `{success,message,data:{proposal,catalogue}}`.
POST `/ai/programs` reçoit `{preferences,proposal}` et renvoie le programme créé.
GET `/ai/options` fournit les valeurs et libellés du formulaire.

## Windows : configurer soi-même la clé

Depuis la racine de sport-vision-training, **copier une seule fois** l’exemple
(la condition préserve un fichier déjà présent), puis ouvrir le fichier local :
```powershell
if (-not (Test-Path backend/.env.local)) { Copy-Item backend/.env.local.example backend/.env.local }
notepad backend/.env.local
```

Remplacer soi-même `COLLEZ_VOTRE_CLE_ICI` après `GEMINI_API_KEY=`.
Conserver `GEMINI_MODEL=gemini-2.5-flash` et `GEMINI_TIMEOUT_SECONDS=45`
(délai autorisé : 5–120 secondes). Format NOM=VALEUR, sans guillemets ni `export`.
Ne jamais copier la clé dans Angular, une conversation ou un fichier versionné.
`.gitignore` exclut `.env.local` ; aucun dépôt Git n’a été initialisé.
Arrêter le backend précédent sur 8082, puis :
```powershell
Set-Location backend
mvn package
powershell -File .\start-local.ps1
```
Dans un autre terminal, depuis la racine : `Set-Location frontend`, puis `npm start`.
Le script passe les variables par l’environnement au processus Java, jamais dans les arguments ;
il restaure ensuite l’environnement du terminal. Relancer après changement de clé.
Sans fichier/clé, le métier démarre ; générer renvoie « Service IA non configuré ».

## Vérification et limites

Deux tests isolés avec Gemini simulé : `mvn "-Dtest=TrainingAiApiTest" test`.
Ils couvrent acceptation/confirmation et rejet d’un identifiant non autorisé sans écriture.
La simulation vérifie la logique de l’application, pas l’accès réel au fournisseur.
**Validation réelle réussie après simplification du schéma : voir le résultat ci-dessous.**
Seules les préférences structurées et les cinq champs autorisés du catalogue sont envoyés.
Aucun nom d’utilisateur, email, historique ou donnée médicale. L’offre gratuite peut utiliser
le contenu transmis pour améliorer les produits Google (voir tarification officielle).
Pas de durée cible, charge prescrite, remplacement, adaptation, RAG, streaming ni agent.
Le mode local conserve son propriétaire commun de démonstration : ajouter authentification,
autorisations et limitation des appels avant exposition publique.

Vérifié le 05/10/2026 : compilations Angular/Spring Boot, deux tests simulés,
réponse HTTP 503 NOT_CONFIGURED, message navigateur précis avec préférences conservées
et affichage à 390 px. Syntaxe du lanceur PowerShell contrôlée sans lire de fichier secret.
Les 2 programmes, 2 séances et 14 exercices préexistants ont été comparés : inchangés.
Ces vérifications initiales précédaient la configuration de la clé.

Diagnostic réel du 05/10/2026 : deux appels, sans retry automatique. Le premier
renvoie HTTP 400 / INVALID_ARGUMENT avec une valeur invalide liée à responseFormat.
Ce bloc est remplacé par responseMimeType et responseJsonSchema, sans changer le
modèle gemini-2.5-flash ni les validations métier. Le second renvoie encore HTTP 400 /
INVALID_ARGUMENT, avec des références minimum, maximum et enum ; la contrainte exacte
n’est pas déterminée par les détails expurgés conservés. Aucun aperçu ni enregistrement.
Backend recompilé avec tests ignorés lors de ce premier diagnostic.

Correction suivante du 05/10/2026 : le message exact de l’ancien HTTP 400 et ses éventuelles
fieldViolations n’avaient pas été conservés ; sa contrainte précise reste inconnue.
Après simplification du schéma fournisseur, un unique nouvel appel réel à gemini-2.5-flash
renvoie HTTP 200. Aperçu validé : une séance, quatre exercices autorisés, répétitions et durée.
Confirmation unique dans le navigateur : programme 33 créé, révision 1. Les programmes 1
(révision 2) et 2 (révision 5) sont conservés. Aucun historique de séance ajouté.
Compilation réussie et cinq tests ciblés réussis (GeminiSchemaTest et TrainingAiApiTest),
sans suite complète ni réseau fournisseur dans les tests. La configuration réellement
sérialisée est inspectée dans target/gemini-generation-config.checked.json, sans données utilisateur.
Les futurs refus journalisent le statut, le code, error.message et fieldViolations après
masquage des secrets, URLs, emails et libellés du catalogue ; aucun en-tête ou corps complet.
