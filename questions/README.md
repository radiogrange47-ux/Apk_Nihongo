# Banques de questions

Chaque banque a son propre dossier :

- `JLPT5/` : questions actuelles, classées par mini-jeu.
- `Mes cours/` : questions tirées des fiches Yuki, actuellement 01 à 10, classées par mini-jeu.
- `JLPT4/` : questions JLPT4 sélectionnées et classées par mini-jeu.
- `Nourriture/` : vocabulaire alimentaire et verbes pour cuisiner, commander et faire les courses.
- `Compteurs/`, `Connecteurs/`, `Démonstratifs/`, `Interrogatifs/`, `Quantifieurs/` : thèmes indépendants sélectionnables depuis l’accueil.

Les fichiers de mini-jeux sont `particles.json`, `vocabulary.json`, `verbs.json` et `conjugation.json`. Chaque question doit contenir `bank` avec l’identifiant correspondant à son dossier : `JLPT5`, `COURSE`, `JLPT4`, `FOOD`, `COUNTERS`, `CONNECTORS`, `DEMONSTRATIVES`, `INTERROGATIVES` ou `DETERMINERS`.

La banque JLPT4 est une sélection pédagogique alignée sur les compétences N4 : vocabulaire (dont katakana), verbes, particules et constructions grammaticales. Le JLPT ne publie plus de liste exhaustive officielle de vocabulaire, kanji et grammaire ; les contenus N4 sont donc des choix éditoriaux, pas une couverture officielle complète.

La banque `Nourriture` ne contient que les mini-jeux Vocabulaire et Verbes. Elle regroupe produits bruts, fruits et légumes, plats japonais, condiments, boissons, vocabulaire de magasin et expressions utiles au restaurant/bar.

Chaque thème de mots et grammaire est une banque indépendante pour pouvoir cocher seulement les thèmes souhaités. Les articles français n’ont pas d’équivalent grammatical direct en japonais : la banque Déterminants présente les démonstratifs, déterminants et quantifieurs qui remplissent des fonctions voisines.

Pour ajouter une banque de questions, créer ou compléter le fichier du mini-jeu dans le dossier de la banque, ajouter son chemin à `manifest.json`, puis lancer `python tools/package_questions.py --expected-version questions-vX.Y.Z`. Le manifeste doit toujours référencer au moins un fichier pour chacun des quatre mini-jeux. Les identifiants de question doivent rester uniques entre les fichiers.
