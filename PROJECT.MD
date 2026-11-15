# Projet : application Android pour apprendre le japonais

## Présentation

L’objectif est de développer une application Android qui aide à apprendre le japonais au moyen de jeux courts et progressifs. Elle doit pouvoir être utilisée hors ligne et suivre les progrès de chaque personne afin de distinguer les notions maîtrisées de celles qui restent à travailler.

La première étape consiste à créer le socle de l’application et le mécanisme permettant de rechercher les mises à jour du logiciel publiées sur le dépôt GitHub du projet. Quatre mini-jeux sont prévus sur ce socle : particules, vocabulaire, vocabulaire des verbes et conjugaison.

## Objectifs

- Permettre de réviser le japonais de façon ludique et régulière.
- Rendre les exercices et les données pédagogiques accessibles sans connexion Internet.
- Enregistrer les réponses et afficher une progression compréhensible.
- Permettre de rechercher et de télécharger, à la demande, les nouvelles versions du logiciel publiées sur le dépôt GitHub du projet.
- Distinguer les mises à jour de l’application des mises à jour de son contenu pédagogique.
- Poser une base à laquelle de nouveaux modes de jeu pourront être ajoutés.

## Fonctionnalités prévues

### Socle de l’application — priorité initiale

La première étape établit une application Android native en Kotlin et Jetpack Compose, avec une structure de projet maintenable, l’identifiant `fr.nihongo.app`, un numéro de version, une configuration de compilation et les écrans et composants communs nécessaires aux évolutions. La configuration initiale cible Android 8.0 (API 26) et compile avec Android SDK 35 et Java 17. La compilation nécessite Android Studio ou un environnement comprenant le JDK 17 et le SDK Android 35.

Le socle comprend :

- un écran d’accueil qui servira ensuite de menu des modes de jeu ;
- un accès aux paramètres et à la recherche de mises à jour ;
- une gestion claire des états de chargement, de succès et d’erreur ;
- une base de stockage local pour les préférences et, ultérieurement, les exercices et statistiques ;
- une séparation entre l’application, les données pédagogiques et le mécanisme de mise à jour ;
- un dossier `questions/` à la racine du dépôt, indépendant du code Kotlin, qui contient les données initiales embarquées dans l’application.

### Mise à jour du logiciel depuis GitHub — priorité initiale

L’application doit pouvoir rechercher, à la demande de la personne, une nouvelle version publiée dans la section **Releases** du dépôt GitHub du projet. Cette fonction concerne le logiciel Android lui-même, et non uniquement les exercices.

Le parcours de mise à jour doit :

1. récupérer les informations de la dernière version publiée, dont son numéro, ses notes de version et l’asset Android compatible ;
2. comparer cette version à celle installée et indiquer clairement si une mise à jour est disponible ;
3. permettre de télécharger le paquet d’installation depuis GitHub avec une connexion sécurisée ;
4. contrôler l’intégrité du fichier téléchargé et laisser Android vérifier la signature de l’application ;
5. demander à la personne de confirmer l’installation via le mécanisme officiel d’Android ; l’application ne tente pas d’installer une mise à jour en silence ;
6. conserver la version déjà installée et signaler explicitement l’échec si la recherche, le téléchargement ou l’installation ne peut pas aboutir.

Les versions publiées doivent respecter la compatibilité Android et être signées de façon cohérente avec l’application installée, faute de quoi Android ne pourra pas effectuer la mise à jour. La vérification des mises à jour est facultative : le logiciel déjà installé doit continuer à fonctionner hors ligne.

Pour être détectée et installable, chaque Release devra avoir un tag de version au format `vMAJEUR.MINEUR.CORRECTIF`, contenir un seul APK universel compatible, et incrémenter le `versionCode` par rapport à l’installation précédente. L’APK devra garder le même identifiant d’application et être signé avec la même clé. La configuration de compilation lit cette clé depuis les variables d’environnement `NIHONGO_KEYSTORE_FILE`, `NIHONGO_KEYSTORE_PASSWORD`, `NIHONGO_KEY_ALIAS` et `NIHONGO_KEY_PASSWORD`. La clé privée et ses mots de passe ne doivent jamais être publiés dans le dépôt.

Pour recevoir ensuite les mises à jour, la première installation sur l’appareil doit être un APK **Release** signé avec cette clé. Une version de développement signée avec la clé debug d’Android ne pourra pas être remplacée par une Release signée avec une autre clé.

Le workflow GitHub Actions `.github/workflows/release-android.yml` teste le projet et fabrique une Release signée lors de la création d’un tag `vMAJEUR.MINEUR.CORRECTIF`. Il publie un seul fichier `app-release.apk` ainsi que son empreinte SHA-256 ; le mécanisme de mise à jour vérifie cette empreinte avant de proposer l’installation.

Avant le premier lancement du workflow, ajouter dans les secrets **Settings > Secrets and variables > Actions** du dépôt :

- `NIHONGO_KEYSTORE_BASE64` : contenu Base64 du fichier keystore de signature ;
- `NIHONGO_KEYSTORE_PASSWORD` : mot de passe du keystore ;
- `NIHONGO_KEY_ALIAS` : alias de la clé ;
- `NIHONGO_KEY_PASSWORD` : mot de passe de la clé.

Créer d’abord le keystore avec **Build > Generate Signed Bundle / APK > APK > Create new** dans Android Studio, et le conserver ainsi que ses mots de passe en lieu sûr. Pour copier sa version Base64 dans le presse-papiers Windows avant de l’ajouter au secret `NIHONGO_KEYSTORE_BASE64` :

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\chemin\nihongo-release.jks")) | Set-Clipboard
```

Le keystore ne doit jamais être commité ou transmis dans le dépôt. Après avoir configuré ces secrets et envoyé les changements sur GitHub, créer et pousser un tag tel que `v0.1.1` pour déclencher la publication. La toute première installation Release sur le téléphone doit remplacer la version de développement, qui est signée différemment ; désinstaller celle-ci efface les données locales de l’application.

### Menu des modes de jeu

Au lancement, l’écran présente les mini-jeux disponibles sous forme de cartes à cocher, dans l’esprit du visuel de référence. La personne peut sélectionner plusieurs mini-jeux, ou utiliser les commandes « Tout » et « Aucun », puis lancer la séance avec un bouton « Démarrer ». Les mini-jeux non développés peuvent apparaître désactivés.

L’interface privilégie un thème sombre, une mise en page épurée, des cartes discrètes et le rouge comme accent. Les consignes redondantes ou évidentes sont évitées ; la phrase, la progression et les réponses restent les éléments visuels principaux.

Chaque mode conserve sa progression séparément. Depuis « Ma progression », on choisit le mini-jeu à consulter et on voit ses résultats ainsi que les notions maîtrisées, à réviser ou à découvrir. Les compteurs individuels des questions servent au tirage et ne sont pas affichés dans cet écran.

### Sélection des mini-jeux et enchaînement des séances — tous les modes

La sélection de l’écran d’accueil porte sur les mini-jeux eux-mêmes (particules, vocabulaire, vocabulaire des verbes et conjugaison), et non sur leurs questions individuelles. La séance enchaîne les mini-jeux cochés sans interruption, dans l’ordre affiché, puis recommence le cycle tant qu’elle n’est pas quittée.

Dans chaque mini-jeu, une série comprend X questions distinctes. Chaque question est tirée au hasard sans répétition dans la série, puis le mini-jeu sélectionné suivant démarre une fois les X questions résolues. Une bonne réponse résout immédiatement la question ; après une mauvaise réponse, la même question reste affichée pendant les essais supplémentaires configurés, puis elle est résolue si ces essais sont épuisés. Une erreur ne remet pas le compteur de série à zéro.

Le nombre X est modifiable dans les paramètres ; sa valeur par défaut est 10 et ne doit pas être confondue avec le seuil des compteurs de maîtrise. Si une banque contient moins de X questions, la série utilise chaque question disponible une seule fois puis passe au jeu suivant. Une nouvelle série commence quand le mini-jeu revient dans le cycle.

Quand toutes les questions des mini-jeux sélectionnés ont atteint le seuil de maîtrise de leur compteur, l’application affiche une information, mais ne termine pas et n’interrompt pas la séance : l’enchaînement des mini-jeux continue.

### Affichage et traduction des contenus japonais — tous les modes

Ces règles s’appliquent à toutes les phrases et à tous les contenus japonais affichés dans l’application, quel que soit le mode de jeu :

- colorer le vocabulaire en bleu, les particules en vert, les verbes en orange et les marqueurs grammaticaux en violet ;
- permettre de toucher chaque mot ou élément pour afficher une fenêtre popup superposée à la phrase, sans déplacer le jeu ;
- afficher dans cette fenêtre le mot en japonais, sa lecture en hiragana et sa traduction ou explication en français ;
- fermer la fenêtre en touchant n’importe où hors de celle-ci, sans bouton de fermeture ;
- afficher les traductions françaises en italique dans toute l’application ;
- permettre de parcourir chaque phrase mot à mot, sans afficher nécessairement sa traduction complète d’emblée ;
- conserver la lisibilité des phrases et permettre de sélectionner individuellement chacun de leurs éléments ;
- fournir une légende et un retour visuel ou textuel à la sélection afin que les catégories ne soient pas identifiées uniquement par la couleur ;
- rendre les annotations, traductions et explications disponibles hors ligne pour les contenus installés.

Les données de contenu japonais doivent pouvoir inclure les éléments ou unités annotés individuellement (texte japonais, lecture en hiragana, catégorie grammaticale et traduction ou explication en français). Cette structure doit être réutilisable par tous les modes de jeu.

### Jeu de particules — première version

Le jeu présente une phrase japonaise contenant une particule manquante et six propositions. La personne choisit la particule qui convient.

Pour chaque question, l’application doit :

- afficher la phrase et les choix de réponse de manière lisible ;
- passer immédiatement à la question suivante après une bonne réponse et afficher la traduction française précédente dans une bannière verte ;
- afficher une invitation rouge à réessayer après une mauvaise réponse ;
- après épuisement des essais, passer à la question suivante en affichant la réponse attendue et la traduction précédente dans une bannière rouge ;
- fournir une explication courte sur l’emploi de la particule ;
- enregistrer le résultat pour le suivi des progrès ;
- proposer dans les paramètres de 0 à 5 essais supplémentaires, avec 1 essai supplémentaire par défaut.

Les exercices doivent inclure la particule attendue, les choix proposés et une explication. Ils peuvent également fournir une traduction complète de la phrase.

### Jeu de vocabulaire — deuxième mode

Le deuxième mode propose un mot japonais présenté en kanji et en hiragana. La personne choisit sa traduction française parmi six propositions. Les mots, lectures et traductions sont disponibles hors ligne ; les règles communes de sélection aléatoire, de progression par jeu, de compteur de maîtrise et de présentation du japonais s’appliquent également à ce mode.

Les premières données du jeu comprennent un petit ensemble de mots courants afin de valider le parcours et de permettre son extension ultérieure.

### Jeu de vocabulaire des verbes — troisième mode

Le troisième mode reprend le format du jeu de vocabulaire, mais présente des verbes japonais. Chaque verbe est affiché en kanji et en hiragana ; la personne choisit sa traduction française parmi six propositions. Les verbes, leurs lectures et leurs traductions sont disponibles hors ligne. La progression et les statistiques de ce mini-jeu restent séparées de celles des autres jeux.

### Jeu de conjugaison — quatrième mode

Le quatrième mode présente une phrase japonaise accompagnée d’un contexte suffisamment clair pour comprendre la situation et déterminer la conjugaison attendue. Une partie du verbe est à compléter ; la personne choisit la bonne terminaison ou forme conjuguée parmi six propositions.

Les questions doivent préciser le verbe et le contexte utiles à la réponse, fournir une correction après chaque choix et enregistrer les résultats dans la progression propre au jeu de conjugaison. Les phrases, lectures, contextes et explications nécessaires sont disponibles hors ligne.

### Statistiques et progression

L’application conserve les résultats localement et présente une synthèse permettant de voir :

- les particules ou notions bien maîtrisées ;
- les notions qui nécessitent davantage de révisions ;
- les résultats et l’évolution au fil des séances.

Les statistiques doivent s’appuyer sur les réponses aux exercices, et non uniquement sur le nombre de parties terminées.

### Sélection des questions et compteurs de maîtrise — tous les modes

Tous les modes choisissent leurs questions aléatoirement parmi les questions installées. Chaque question possède un compteur de maîtrise local : une réponse correcte l’augmente de 1 et une réponse incorrecte le diminue de 1, sans descendre sous zéro. Lorsque ce compteur atteint le seuil X, le poids de la question dans le tirage passe à 5 % du poids normal ; elle reste donc possible, mais apparaît beaucoup moins souvent. Le tirage est fait à chaque nouvelle question et autorise les répétitions au cours d’une séance.

Dans les paramètres, la personne peut saisir le seuil X (entier strictement positif, 25 par défaut) et réinitialiser les compteurs de toutes les questions. Cette réinitialisation conserve l’historique des réponses et les statistiques associées.

Ce seuil de maîtrise est distinct du nombre de questions distinctes X nécessaires pour terminer une série de mini-jeu.

### Fonctionnement hors ligne

Les exercices déjà installés, le jeu et les statistiques doivent rester accessibles sans connexion. La connexion Internet est facultative et sert à rechercher des mises à jour du logiciel ou des données pédagogiques.

### Mise à jour des données pédagogiques depuis GitHub

Cette fonction est distincte de la mise à jour du logiciel. La personne peut demander à l’application de récupérer de nouveaux exercices ou des mises à jour du contenu pédagogique depuis le dépôt GitHub du projet. Cette action est manuelle et nécessite une connexion Internet.

Les données sont maintenues dans `questions/`, classées d’abord par banque puis par mini-jeu. Chaque entrée JSON porte un champ `bank` (`JLPT5`, `COURSE`, `JLPT4` ou `FOOD`) ; pour ajouter du contenu, placer les nouvelles questions dans le fichier du mini-jeu correspondant sous la banque choisie et ajouter le chemin au manifeste. Les banques actuellement disponibles sont `questions/JLPT5/`, `questions/Mes cours/`, `questions/JLPT4/` et `questions/Nourriture/`.

- `manifest.json` décrit la version du format, la version du contenu et les fichiers attendus ;
- `JLPT5/particles.json`, `JLPT5/vocabulary.json`, `JLPT5/verbs.json` et `JLPT5/conjugation.json` contiennent les questions JLPT5 de chaque mini-jeu ;
- chaque question possède un identifiant stable `id`, une notion `topic`, ses données de jeu et, facultativement, un niveau `level` et une provenance `source` ;
- les nouvelles questions préparées pour le niveau N5 portent `level: "JLPT N5"` et une provenance qui précise que la sélection est pédagogique et non officielle ;
- les mots japonais des phrases sont représentés par des éléments annotés (japonais, hiragana, français et catégorie grammaticale), réutilisables par tous les modes.

Le format est versionné séparément de l’application. La version initiale est embarquée comme ressources dans l’APK pour garantir le fonctionnement hors ligne. Une mise à jour du contenu est publiée dans une Release GitHub distincte, sous un tag `questions-vMAJEUR.MINEUR.CORRECTIF`, avec un unique paquet `questions.zip`. Le workflow `.github/workflows/release-questions.yml` valide et publie ce paquet ; le script `tools/package_questions.py` vérifie les données et le construit. Les Releases de contenu ne contiennent pas d’APK. La recherche de mise à jour du logiciel ne considère que les tags `vMAJEUR.MINEUR.CORRECTIF` et ignore ceux des questions.

Avant chaque publication de contenu, `contentVersion` dans `manifest.json` est incrémentée et doit correspondre au tag `questions-v...` créé sur ce commit. Une correction de donnée ou un ajout incrémente au minimum la version corrective ; une évolution incompatible du format des fichiers incrémente `schemaVersion` et nécessite d’abord une mise à jour de l’application.

À la demande de la personne, l’application recherche la Release de questions la plus récente, télécharge le paquet en HTTPS, vérifie son empreinte SHA-256 publiée par GitHub, la structure ZIP, le manifeste, la version du format et la validité de toutes les questions. Elle n’active le paquet qu’après validation complète. Les données activées sont copiées dans le stockage privé de l’application ; une version précédente est conservée pendant le remplacement afin de pouvoir revenir en arrière en cas d’échec. Après une mise à jour réussie, les questions installées restent accessibles hors ligne. Si le réseau, le téléchargement, la validation ou l’installation échoue, l’application conserve le contenu utilisable déjà installé et signale l’erreur.

À chaque installation de contenu, les compteurs locaux sont synchronisés avec les identifiants des questions, dans la même progression par jeu :

- pour une question dont le même identifiant reste présent, les compteurs de maîtrise, de réponses et de réussite ne changent pas ;
- pour une nouvelle question, ces compteurs sont créés à zéro ;
- pour une question retirée, ses compteurs et son historique individuel sont supprimés ;
- si une question est réécrite pour tester une notion différente, elle reçoit un nouvel identifiant ; une correction de forme ou de traduction peut conserver l’identifiant existant.

Les statistiques étant calculées à partir des questions actuellement installées, le retrait d’une question retire également sa contribution aux totaux affichés. Un historique global indépendant des questions n’est pas requis pour cette première version.

Les fiches de cours sont une source de préparation du contenu, et non un format que l’application doit interpréter directement. Le processus prévu est : fournir ou sélectionner une source, transformer les notions en questions JSON adaptées aux quatre schémas, renseigner le niveau et la provenance lorsqu’ils sont connus, lancer la validation automatique, relire les réponses et distracteurs, puis publier une nouvelle version du contenu. L’assistance par IA peut accélérer la structuration, mais le contenu pédagogique doit être relu avant publication. Les sources protégées ne sont utilisées que dans les limites de leurs droits ; les ressources ouvertes ou les fiches fournies par la personne sont privilégiées.

Les quatre schémas sont contrôlés automatiquement : identifiants non vides et uniques dans tout le jeu de données, six choix distincts avec la bonne réponse incluse, champs nécessaires présents et indices des éléments grammaticaux valides. Une question de vocabulaire peut par exemple inclure `id`, `topic`, `kanji`, `hiragana`, `answer`, `choices`, ainsi que les champs facultatifs `level` et `source`. Une question de phrase décrit chaque élément japonais séparément afin de permettre sa coloration et sa traduction au toucher. Une modification du contenu ne change pas `schemaVersion` ; une évolution incompatible du format exige une version de logiciel qui prend en charge le nouveau schéma.

### Contenus pédagogiques

### Contenu des cours Yuki

La version de contenu `1.3.1` comprend 235 questions issues des fiches 01 à 10, plus trois mots en katakana ajoutés depuis la fiche 08. La banque « Mes cours » compte 24 questions en particules, 148 en vocabulaire, 41 en verbes et 25 en conjugaison (238 au total). Les questions couvrent notamment les métiers, l’école, les jours et périodes, la météo, les loisirs, les particules de lieu et de temps, ainsi que les formes polies passées et la forme en て. Les traductions ont été corrigées ou précisées selon le contexte : `しゅうまつ` signifie « week-end », `はたらきます` « travailler », et `じゅんびします` « préparer ».

### Contenu pédagogique JLPT N5

Le niveau N5 vise la compréhension du japonais élémentaire, notamment le vocabulaire et la grammaire de base. La [description officielle des niveaux JLPT](https://www.jlpt.jp/e/about/levelsummary.html) présente des compétences de lecture et d’écoute ; les banques de cette application sont une sélection pédagogique, et non une liste officielle exhaustive ni une garantie de réussite à l’examen.

La banque JLPT5, embarquée en version `1.4.0`, comprend 217 questions : 22 en particules, 108 en vocabulaire, 65 en verbes et 22 en conjugaison. La version `1.5.0` ajoute 246 questions JLPT4 : 28 en particules, 112 en vocabulaire, 56 en verbes et 50 en conjugaison. La version `1.5.1` ajoute 161 questions thématiques Nourriture : 125 en vocabulaire et 36 verbes pour les repas, boissons, cuisine, restaurant/bar et courses alimentaires. Cette sélection N4 couvre notamment les katakana usuels, les verbes et des constructions grammaticales de niveau élémentaire avancé. Chaque question porte un ID stable et une provenance pédagogique. Le JLPT ne publie pas de liste officielle exhaustive de vocabulaire et grammaire par niveau ; le contenu constitue donc une sélection de travail susceptible d’être enrichie. Le JLPT comprend aussi des compétences de lecture et d’écoute qui ne sont pas encore évaluées par les quatre mini-jeux actuels.

## Parcours principal

1. La personne ouvre l’application et arrive sur l’écran d’accueil.
2. Elle peut demander une recherche de nouvelle version du logiciel depuis l’application.
3. Si une version est disponible, l’application présente son numéro et ses notes de version, puis propose de la télécharger et de lancer l’installation avec confirmation Android.
4. Depuis l’écran d’accueil, la personne coche un ou plusieurs mini-jeux — particules, vocabulaire, vocabulaire des verbes ou conjugaison — démarre la séance et les enchaîne après X questions distinctes résolues dans chacun.
5. La personne consulte séparément les statistiques et les notions à réviser de chaque mini-jeu.
6. La personne peut rechercher et installer séparément une mise à jour des questions ; l’application et les contenus déjà installés restent utilisables hors ligne.

## Première version — socle et mini-jeux

La première livraison doit comprendre :

- une application Android installable et versionnée ;
- un écran d’accueil de sélection des mini-jeux à cocher, avec les commandes « Tout », « Aucun » et un bouton « Démarrer » ;
- quatre mini-jeux sélectionnables : particules, vocabulaire, vocabulaire des verbes et conjugaison ;
- un jeu de vocabulaire proposant un mot en kanji et hiragana avec six traductions françaises possibles ;
- un jeu de vocabulaire des verbes, de même format, avec six traductions françaises possibles ;
- un jeu de conjugaison proposant une phrase en contexte et six terminaisons ou formes conjuguées au choix ;
- une banque hors ligne de questions N5 pour chacun des quatre mini-jeux (version de contenu initiale `1.1.0`) ;
- un écran de progression consultable indépendamment pour chaque mode ;
- six propositions de réponse pour chaque question de l’application ;
- des tirages aléatoires pondérés par les compteurs de maîtrise, sans répétition d’une question dans une même série ;
- un réglage du seuil de maîtrise (25 par défaut) et une commande pour réinitialiser tous les compteurs ;
- un réglage distinct du nombre X de questions distinctes par mini-jeu avant de passer au suivant, avec une barre de progression ;
- les quatre banques initiales fournies sous forme de fichiers JSON validés dans `questions/` et embarquées comme ressources hors ligne ;
- une recherche et une installation manuelles des Releases de contenu GitHub, indépendantes des Releases APK ;
- une synchronisation par identifiant stable qui préserve les compteurs des questions toujours présentes, initialise les nouvelles et supprime ceux des questions retirées ;
- la validation du manifeste, du format, des choix et des empreintes avant activation d’un paquet, avec retour à la version précédente en cas d’échec ;
- un enchaînement continu des mini-jeux sélectionnés et une information non bloquante lorsque tous leurs compteurs de maîtrise atteignent le seuil ;
- l’affichage coloré des catégories et une popup de traduction mot à mot indiquant le japonais, sa lecture en hiragana et le français en italique ;
- les corrections et l’enregistrement local des réponses ;
- un écran de statistiques avec les notions à réviser et maîtrisées ;
- une navigation extensible vers les futurs modes de jeu ;
- un accès à la recherche manuelle des mises à jour depuis les Releases GitHub du projet ;
- l’affichage du numéro de version installée, de la version disponible et des notes de version ;
- le téléchargement contrôlé du paquet Android et le passage par la confirmation d’installation du système ;
- des messages explicites en cas d’absence de mise à jour, d’absence de réseau ou d’échec ;
- le maintien du fonctionnement du jeu et des contenus installés hors ligne.

## Critères d’acceptation de la première version

- L’application Android peut être compilée, installée et lancée avec un numéro de version identifiable.
- Le jeu de particules est accessible depuis le menu principal et propose une série de questions jouable hors ligne.
- Les quatre mini-jeux — particules, vocabulaire, vocabulaire des verbes et conjugaison — peuvent être sélectionnés depuis l’écran d’accueil.
- Le mode vocabulaire affiche un mot en kanji et hiragana et propose six traductions possibles.
- Le mode vocabulaire des verbes affiche un verbe en kanji et hiragana et propose six traductions possibles.
- Le mode conjugaison présente une phrase avec un contexte compréhensible et propose six terminaisons ou formes conjuguées.
- L’écran d’accueil permet de cocher un ou plusieurs mini-jeux, de tout sélectionner ou tout désélectionner, puis de démarrer la séance.
- Les mini-jeux sélectionnés s’enchaînent continuellement ; chaque série présente X questions distinctes au maximum une fois, puis passe au jeu suivant, et la barre indique la progression.
- Une mauvaise réponse conserve la question en cours pendant les tentatives supplémentaires configurées ; une fois la question résolue, elle n’est pas tirée de nouveau dans la série.
- Si la banque contient moins de X questions, toutes ses questions sont présentées une seule fois avant le passage au mini-jeu suivant.
- La maîtrise de toutes les questions sélectionnées affiche une information sans interrompre l’enchaînement.
- La progression et les statistiques sont distinctes et consultables par jeu, avec les notions à réviser ; les compteurs individuels des questions ne sont pas affichés.
- Chaque question de tous les modes propose exactement six choix de réponse distincts.
- Chaque réponse correcte augmente le compteur de sa question, chaque réponse incorrecte le diminue sans passer sous zéro ; à partir du seuil réglé, le poids de la question est réduit à 5 %.
- Les questions sont tirées au hasard sans remise dans une série de X questions distinctes ; le seuil de maîtrise reste réglable (25 par défaut) et la réinitialisation remet tous les compteurs à zéro sans effacer l’historique des réponses.
- Les questions N5 sont réparties entre les quatre jeux et renseignent leur niveau et leur provenance ; la couverture est pédagogique, évolutive et ne constitue pas une liste officielle exhaustive du JLPT.
- Chaque mot ou élément japonais est coloré selon sa catégorie et ouvre au toucher une popup superposée présentant le japonais, sa lecture en hiragana et le français en italique ; toucher hors de la popup la ferme.
- Chaque réponse reçoit une correction ; les réponses restent enregistrées après fermeture de l’application.
- Les statistiques distinguent les notions à réviser de celles maîtrisées.
- Une personne peut déclencher une recherche de version depuis l’application.
- L’application distingue une version déjà à jour d’une version plus récente et présente les notes de version disponibles.
- Le téléchargement d’une version publiée est contrôlé et l’installation requiert la confirmation du système Android.
- Un échec de mise à jour est signalé sans supprimer ni désactiver la version installée.
- L’application installée reste lançable sans connexion Internet.
- Une mise à jour de questions publiée dans une Release de contenu est installable sans reconstruire ni réinstaller l’APK.
- Les compteurs d’une question identifiée comme toujours présente sont inchangés après une mise à jour, les nouvelles questions commencent à zéro et les données individuelles des questions retirées sont supprimées.
- L’application ignore les Releases `questions-v...` lors de la recherche d’un nouvel APK et ignore les Releases `v...` lors de la recherche des questions.

## Étapes suivantes

- Ajouter d’autres jeux et thèmes de vocabulaire ou de grammaire.
- Étendre par lots la couverture N5 et ajouter des exercices dédiés à la compréhension écrite et orale.
- Ajouter des filtres et parcours par niveau à partir des métadonnées `level`.
- Améliorer l’outillage de préparation des questions à partir de fiches de cours et de ressources pédagogiques vérifiées.
- Enrichir les séances de révision et les statistiques.
