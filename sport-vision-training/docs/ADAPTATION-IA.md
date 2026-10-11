# Bilan et adaptation IA

Depuis un programme actif : **Bilan et adaptation IA**, puis **Proposer une adaptation**.
Page : `http://127.0.0.1:4201/entrainement/programmes/:id/adaptation`.

Java analyse au maximum les 20 dernières séances terminées du propriétaire et de la
révision courante. Les séances abandonnées, en cours, d'autres programmes ou d'anciennes
révisions sont exclues : les prescriptions comparées doivent être identiques.
Chaque exercice doit avoir des séries réalisées dans au moins deux séances pour être
éligible. Les exercices sans données suffisantes restent inchangés. Le bilan expose les
séries réalisées/ignorées, les répétitions ou secondes réalisées et prescrites, leur écart,
et la couverture des charges renseignées. Un écart ne constitue pas une preuve de progression.

GeminiClient existant (modèle par défaut `gemini-2.5-flash`, configuration existante conservée)
reçoit uniquement des positions locales, types de mesure, nombres et options autorisées.
Aucun nom, identifiant de base/utilisateur, objectif libre, commentaire, date ou historique
brut n'est envoyé. Sans option prudente, aucun appel n'est effectué.

Java produit les options : baisse d'une série, baisse de cible d'environ 10 % plafonnée
à 2 répétitions ou 10 secondes, repos augmenté de 15 secondes. Lorsque toutes les cibles
sont atteintes sans série ignorée, une hausse limitée de cible est possible (1 répétition,
ou environ 10 % et au maximum 10 secondes). Une charge prescrite doit être documentée et
atteinte avant toute hausse ; la hausse de charge est plafonnée à 5 % et 2,5 kg et ne
peut dépasser la charge minimale déjà réalisée. Aucune charge manquante n'est inventée.
Gemini sélectionne au maximum trois options, une par prescription ; une liste vide est
valide. Java refuse tout autre champ, option inconnue, doublon ou valeur non entière.

Endpoints sous `/api/training/ai/programs/{id}/adaptation` :

- `GET ?expectedRevision=1` : bilan Java, sans Gemini.
- `POST {"expectedRevision":1}` : proposition et comparaison avant/après.
- `POST /confirm {"expectedRevision":1,"proposalId":"UUID"}` : nouvelle révision.

La proposition validée est conservée 30 minutes dans `training_adaptation_proposals`,
liée au propriétaire serveur, programme, révision et empreinte du bilan. Le navigateur
ne transmet jamais de nouvelles prescriptions à la confirmation. Sous verrou du
programme et dans une transaction, Java revérifie la proposition, l'empreinte et les
options puis utilise le service de versionnement existant. Une confirmation concurrente
ou répétée après succès reçoit 409 ; une seule révision est créée. Une nouvelle séance
modifiant le bilan, une proposition expirée, un archivage ou une autre révision impose
de recalculer. Les instantanés des séances et les anciennes révisions restent intacts.
Les propositions expirées restent stockées, mais sont inutilisables.

Angular gère chargement, insuffisance de données, erreurs fournisseur/réseau, 409,
annulation et confirmation explicite. Après une perte réseau à la confirmation, consulter
la fiche ou recharger le bilan avant de recommencer. Aucun appel réel n'est nécessaire
pour les tests : `mvn --offline "-Dtest=TrainingAdaptationApiTest" test`, Gemini simulé,
H2 en mémoire et profil `test`. Build : `npm run build -- --configuration production`.

En profil local, la table est ajoutée au prochain démarrage par le `ddl-auto=update`
déjà configuré. Ne pas réinitialiser H2. L'identité reste celle fournie par le raccordement
serveur existant ; le mode démonstration local n'est pas une authentification de production.

Vérification du 11/10/2026 : 11 tests d'adaptation réussis ; suite backend complète,
45 tests réussis, aucun échec. Build Angular de production réussi. Gemini est simulé
dans les tests IA ; aucun appel fournisseur réel ni modification de la base H2 locale.
