# Banques de questions

Chaque banque a son propre dossier :

- `JLPT5/` : questions actuelles, classées par mini-jeu.
- `Mes cours/` : questions tirées des fiches Yuki, actuellement 01 à 12, classées par mini-jeu.
- `JLPT4/` : questions JLPT4 sélectionnées et classées par mini-jeu.
- `Nourriture/` : vocabulaire alimentaire et verbes pour cuisiner, commander et faire les courses.
- `Compteurs/`, `Connecteurs/`, `Démonstratifs/`, `Interrogatifs/`, `Quantifieurs/` : thèmes indépendants sélectionnables depuis l’accueil.

Les fichiers de mini-jeux sont `particles.json`, `vocabulary.json`, `verbs.json` et `conjugation.json`. Chaque question doit contenir `bank` avec l’identifiant correspondant à son dossier : `JLPT5`, `COURSE`, `JLPT4`, `FOOD`, `COUNTERS`, `CONNECTORS`, `DEMONSTRATIVES`, `INTERROGATIVES` ou `DETERMINERS`.

Les fichiers gardent six choix sources avec la réponse correcte. En jeu, les QCM affichent quatre propositions : la bonne réponse et trois distracteurs tirés au hasard. Le jeu des paires garde six paires par grille.

La banque JLPT4 est une sélection pédagogique alignée sur les compétences N4 : vocabulaire (dont katakana), verbes, particules et constructions grammaticales. Le JLPT ne publie plus de liste exhaustive officielle de vocabulaire, kanji et grammaire ; les contenus N4 sont donc des choix éditoriaux, pas une couverture officielle complète.

La banque `Nourriture` ne contient que les mini-jeux Vocabulaire et Verbes. Elle regroupe produits bruts, fruits et légumes, plats japonais, condiments, boissons, vocabulaire de magasin et expressions utiles au restaurant/bar.

Les banques `Compteurs`, `Connecteurs`, `Démonstratifs`, `Interrogatifs` et `Quantifieurs` contiennent des mots à apprendre par traduction. Elles se sélectionnent dans le parcours **Vocabulaire**, même si ces mots ont un sens grammatical. Les articles français n’ont pas d’équivalent grammatical direct en japonais : la banque Déterminants présente les démonstratifs, déterminants et quantifieurs qui remplissent des fonctions voisines.

Pour ajouter une banque de questions, créer ou compléter le fichier du mini-jeu dans le dossier de la banque, ajouter son chemin à `manifest.json`, puis lancer `python tools/package_questions.py --expected-version questions-vX.Y.Z`. Le manifeste doit toujours référencer au moins un fichier pour chacun des quatre mini-jeux. Les identifiants de question doivent rester uniques entre les fichiers.

## Parcours de l’application 2.0

L’accueil propose trois parcours dans une seule application :

- **Vocabulaire** : mots, verbes, compteurs et paires japonaises. Tous les mots à traduire y figurent, notamment les interrogatifs, démonstratifs et connecteurs.
- **Grammaire** : exercices d’application dans une phrase, comme choisir la bonne particule. Les banques de mots outils restent dans Vocabulaire tant que leurs questions demandent seulement une traduction.
- **Conjugaison** : exercices pour choisir ou écrire la forme verbale attendue.

Le parcours détermine le type de travail : traduire les mots dans Vocabulaire, appliquer une règle en contexte dans Grammaire, et produire la forme verbale dans Conjugaison. Les banques affichées sont limitées à celles qui contiennent des questions adaptées au parcours.

### Groupe actif de notions

La séance continue de passer au mini-jeu suivant après 10 questions par défaut. En parallèle, elle travaille au plus 15 notions actives choisies dans l’ensemble des banques sélectionnées et des mini-jeux cochés. Les notions partagées entre plusieurs formats (par exemple un mot vu en traduction, à l’écoute et dans Paires) ne comptent qu’une fois.

Le groupe actif est déterminé par la progression persistée : les notions non acquises y restent, et le groupe est renouvelé au début du mini-jeu suivant, après la série en cours. Si le cycle se termine, le groupe est renouvelé au démarrage du cycle suivant. L’ordre de sélection est pseudo-aléatoire mais stable pour un même choix de parcours, banques et jeux, afin que le groupe ne change pas à chaque recomposition de l’écran. Les questions à l’intérieur du groupe restent tirées aléatoirement.

Les notions acquises restent consultables et peuvent revenir en révision selon le pourcentage configuré. Pour préserver la priorité des notions à apprendre, le tirage présente au maximum 15 notions acquises en révision à la fois. Ce quota de révision est distinct des 15 notions actives.

### Mini-jeux disponibles et pistes à développer

Jeux actuellement proposés par parcours :

- **Vocabulaire** : quatre mini-jeux sélectionnables sur le même ensemble de mots et de verbes (écoute puis traduction, japonais vers français, français vers japonais, rappel écrit), ainsi que **Paires**. Les questions de compteurs sont intégrées à Vocabulaire et leurs choix restent limités aux autres compteurs.
- **Grammaire** : choix de la particule manquante dans une phrase.
- **Conjugaison** : choix ou saisie d’une terminaison, ou choix de la phrase japonaise correspondant à une consigne française.

Idées pour de futurs mini-jeux, à concevoir avant toute implémentation :

- **Vocabulaire** : retrouver un mot à partir d’un indice ou d’une courte définition.
- **Grammaire** : compléter une phrase avec un connecteur ou un mot grammatical ; remettre les segments d’une phrase dans l’ordre ; repérer et corriger une erreur grammaticale.
- **Conjugaison** : transformer une forme donnée (présent, passé, négation, forme en て) ; choisir la forme adaptée à une situation ; écrire la phrase conjuguée complète.

Les parcours filtrent le catalogue installé sans dupliquer les questions. Les quatre formats de vocabulaire sont des mini-jeux cochables distincts et utilisent le même ensemble de mots et de verbes. La séance suit l’ordre d’affichage des jeux cochés, pose X questions dans le premier, puis X dans le suivant, et affiche un bilan après le dernier. X vaut 10 par défaut et se règle dans les paramètres ; si un jeu a moins de questions disponibles, il utilise celles qui restent. La maîtrise d’un mot est partagée entre ces quatre formats.
