package fr.nihongo.app

/** Top-level paths over one catalog; each path exposes only games matching its learning goal. */
internal enum class LearningTrack(
    val title: String,
    val description: String,
    val symbol: String
) {
    VOCABULARY(
        title = "Vocabulaire",
        description = "Mots, verbes, compteurs et associations",
        symbol = "語"
    ),
    GRAMMAR(
        title = "Grammaire",
        description = "Appliquer les règles grammaticales dans des phrases",
        symbol = "文法"
    ),
    CONJUGATION(
        title = "Conjugaison",
        description = "Choisir ou écrire la forme verbale attendue",
        symbol = "活"
    );

    fun catalog(catalog: QuestionCatalog): QuestionCatalog = catalog.forTrack(this)

    fun gamesIn(catalog: QuestionCatalog): List<LearningGame> {
        val games = when (this) {
            VOCABULARY -> listOf(
                LearningGame.VOCABULARY_AUDIO,
                LearningGame.VOCABULARY,
                LearningGame.VOCABULARY_FRENCH_TO_JAPANESE,
                LearningGame.VOCABULARY_WRITE,
                LearningGame.PAIRS
            )
            GRAMMAR -> listOf(LearningGame.PARTICLES)
            CONJUGATION -> listOf(LearningGame.CONJUGATION)
        }
        return games.filter { catalog.questionsFor(it).isNotEmpty() }
    }
}

internal fun QuestionCatalog.forTrack(track: LearningTrack): QuestionCatalog = when (track) {
    LearningTrack.VOCABULARY -> copy(
        particles = emptyList(),
        conjugation = emptyList()
    )
    LearningTrack.GRAMMAR -> copy(
        particles = particles,
        vocabulary = emptyList(),
        verbs = emptyList(),
        conjugation = emptyList()
    )
    LearningTrack.CONJUGATION -> copy(
        particles = emptyList(),
        vocabulary = emptyList(),
        verbs = emptyList()
    )
}
