# Projet : application Android pour apprendre le japonais

## Présentation

L’objectif est de développer une application Android qui aide à apprendre le japonais au moyen de jeux courts et progressifs. Elle doit pouvoir être utilisée hors ligne et suivre les progrès de chaque personne afin de distinguer les notions maîtrisées de celles qui restent à travailler.

La première étape consiste à créer le socle de l’application et le mécanisme permettant de rechercher les mises à jour du logiciel publiées sur le dépôt GitHub du projet. Les modes de jeu et le premier jeu de particules seront développés sur ce socle.

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
- une séparation entre l’application, les données pédagogiques et le mécanisme de mise à jour.

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

### Menu des modes de jeu

Au lancement, l’application affiche un menu présentant les modes disponibles. Le premier mode est jouable ; les modes qui ne sont pas encore développés peuvent être indiqués comme à venir.

### Affichage et traduction des contenus japonais — tous les modes

Ces règles s’appliquent à toutes les phrases et à tous les contenus japonais affichés dans l’application, quel que soit le mode de jeu :

- colorer le vocabulaire en bleu, les particules en vert, les verbes en orange et les marqueurs grammaticaux en violet ;
- permettre de toucher chaque mot ou élément pour afficher une bulle contenant sa traduction ou son explication en français ;
- permettre de parcourir chaque phrase mot à mot, sans afficher nécessairement sa traduction complète d’emblée ;
- conserver la lisibilité des phrases et permettre de sélectionner individuellement chacun de leurs éléments ;
- fournir une légende et un retour visuel ou textuel à la sélection afin que les catégories ne soient pas identifiées uniquement par la couleur ;
- rendre les annotations, traductions et explications disponibles hors ligne pour les contenus installés.

Les données de contenu japonais doivent pouvoir inclure les éléments ou unités annotés individuellement (texte japonais, catégorie grammaticale et traduction ou explication en français). Cette structure doit être réutilisable par tous les modes de jeu.

### Jeu de particules — première version

Le jeu présente une phrase japonaise contenant une particule manquante et plusieurs réponses possibles. La personne choisit la particule qui convient.

Pour chaque question, l’application doit :

- afficher la phrase et les choix de réponse de manière lisible ;
- indiquer si la réponse est correcte ;
- fournir une explication courte sur l’emploi de la particule ;
- enregistrer le résultat pour le suivi des progrès ;
- permettre de passer à la question suivante.

Les exercices doivent inclure la particule attendue, les choix proposés et une explication. Ils peuvent également fournir une traduction complète de la phrase.

### Statistiques et progression

L’application conserve les résultats localement et présente une synthèse permettant de voir :

- les particules ou notions bien maîtrisées ;
- les notions qui nécessitent davantage de révisions ;
- les résultats et l’évolution au fil des séances.

Les statistiques doivent s’appuyer sur les réponses aux exercices, et non uniquement sur le nombre de parties terminées.

### Fonctionnement hors ligne

Les exercices déjà installés, le jeu et les statistiques doivent rester accessibles sans connexion. La connexion Internet est facultative et sert à rechercher des mises à jour du logiciel ou des données pédagogiques.

### Mise à jour des données pédagogiques depuis GitHub

Cette fonction est distincte de la mise à jour du logiciel. La personne peut demander à l’application de récupérer de nouveaux exercices ou des mises à jour du contenu pédagogique depuis le dépôt GitHub du projet. Cette action est manuelle et nécessite une connexion Internet.

Après téléchargement, les données doivent être conservées sur l’appareil pour rester disponibles hors ligne. Une mise à jour ne doit pas rendre les exercices inutilisables si le téléchargement échoue ; l’application conserve alors le contenu déjà installé et signale le problème.

Le format des données et leur stratégie de versionnement restent à définir lors de la mise en œuvre. Le contenu pédagogique doit pouvoir être mis à jour indépendamment d’une nouvelle version du logiciel lorsque cela est possible.

## Parcours principal

1. La personne ouvre l’application et arrive sur l’écran d’accueil.
2. Elle peut demander une recherche de nouvelle version du logiciel depuis l’application.
3. Si une version est disponible, l’application présente son numéro et ses notes de version, puis propose de la télécharger et de lancer l’installation avec confirmation Android.
4. Une fois le socle en place, la personne pourra accéder au menu des modes de jeu, lancer le jeu de particules et consulter ses résultats.
5. L’application et les contenus déjà installés restent utilisables hors ligne.

## Première étape — socle et mise à jour du logiciel

La première livraison doit comprendre :

- une application Android installable et versionnée ;
- un écran d’accueil et une structure de navigation extensible ;
- un accès à la recherche manuelle des mises à jour depuis les Releases GitHub du projet ;
- l’affichage du numéro de version installée, de la version disponible et des notes de version ;
- le téléchargement contrôlé du paquet Android et le passage par la confirmation d’installation du système ;
- des messages explicites en cas d’absence de mise à jour, d’absence de réseau ou d’échec ;
- le maintien du fonctionnement de la version installée hors ligne.

Le menu complet, le jeu de particules, les statistiques et la mise à jour des exercices seront réalisés dans les étapes suivantes, sur le socle validé.

## Critères d’acceptation de la première étape

- L’application Android peut être compilée, installée et lancée avec un numéro de version identifiable.
- Une personne peut déclencher une recherche de version depuis l’application.
- L’application distingue une version déjà à jour d’une version plus récente et présente les notes de version disponibles.
- Le téléchargement d’une version publiée est contrôlé et l’installation requiert la confirmation du système Android.
- Un échec de mise à jour est signalé sans supprimer ni désactiver la version installée.
- L’application installée reste lançable sans connexion Internet.

## Étape suivante — premier jeu

Après validation du socle, le premier jeu doit comprendre un menu des modes de jeu, un ensemble initial d’exercices de particules, les règles communes d’affichage et de traduction, les corrections, l’enregistrement local des résultats et un écran de statistiques élémentaire. Le jeu et les contenus déjà installés doivent fonctionner hors ligne.

## Évolutions possibles

- Ajouter d’autres jeux et thèmes de vocabulaire ou de grammaire.
- Proposer des séances de révision ciblées sur les notions moins maîtrisées.
- Enrichir les statistiques et les options de progression.
- Améliorer la gestion des versions et l’affichage des nouveautés disponibles.