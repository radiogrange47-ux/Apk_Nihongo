# Banques de questions

Chaque banque a son propre dossier :

- `JLPT5/` : questions actuelles, classées par mini-jeu.
- `Mes cours/` : questions tirées des fiches Yuki 01 à 04, classées par mini-jeu.
- `JLPT4/` : futures questions JLPT4, classées par mini-jeu.

Les fichiers de mini-jeux sont `particles.json`, `vocabulary.json`, `verbs.json` et `conjugation.json`. Chaque question doit contenir `bank` avec l’identifiant correspondant : `JLPT5`, `COURSE` ou `JLPT4`.

Pour ajouter une banque de questions, créer ou compléter le fichier du mini-jeu dans le dossier de la banque, ajouter son chemin à `manifest.json`, puis lancer `python tools/package_questions.py --expected-version questions-vX.Y.Z`. Le manifeste doit toujours référencer au moins un fichier pour chacun des quatre mini-jeux. Les identifiants de question doivent rester uniques entre les fichiers.
