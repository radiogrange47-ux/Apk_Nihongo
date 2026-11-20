package fr.nihongo.app

internal enum class WordCategory {
    VOCABULARY,
    PARTICLE,
    VERB,
    GRAMMAR_MARKER,
    ADJECTIVE
}

internal enum class LearningGame(val title: String) {
    PARTICLES("Jeu des particules"),
    VOCABULARY("Japonais → français"),
    VOCABULARY_AUDIO("Écoute et traduction"),
    VOCABULARY_FRENCH_TO_JAPANESE("Français → japonais"),
    VOCABULARY_WRITE("Écrire en japonais"),
    VERBS("Vocabulaire des verbes"),
    CONJUGATION("Conjugaison"),
    PAIRS("Paires"),
    COUNTER_CHALLENGE("Défi des compteurs")
}

internal enum class QuestionBank(val title: String) {
    JLPT5("JLPT5"),
    COURSE("Mes cours"),
    JLPT4("JLPT4"),
    FOOD("Nourriture"),
    COUNTERS("Compteurs"),
    CONNECTORS("Mots connecteurs"),
    DEMONSTRATIVES("Démonstratifs"),
    INTERROGATIVES("Mots interrogatifs"),
    DETERMINERS("Déterminants & quantifieurs")
}

internal interface LearningQuestion {
    val id: String
    val topic: String
    val level: String?
    val source: String?
    val bank: QuestionBank
    val prompt: String
}

internal data class PhraseToken(
    val japanese: String,
    val hiragana: String,
    val french: String,
    val category: WordCategory
) {
    init {
        require(japanese.isNotBlank())
        require(hiragana.isNotBlank())
        require(french.isNotBlank())
    }
}

internal data class ParticleExercise(
    override val id: String,
    override val topic: String,
    val tokens: List<PhraseToken>,
    val missingTokenIndex: Int,
    val choices: List<String>,
    val answer: String,
    val explanation: String,
    val translation: String,
    override val level: String? = null,
    override val source: String? = null,
    override val bank: QuestionBank = QuestionBank.JLPT5
) : LearningQuestion {
    override val prompt: String
        get() = tokens.mapIndexed { index, token ->
            if (index == missingTokenIndex) "..." else token.japanese
        }.joinToString("")

    init {
        require(id.isNotBlank())
        require(topic.isNotBlank())
        require(missingTokenIndex in tokens.indices)
        require(
            tokens[missingTokenIndex].category == WordCategory.PARTICLE ||
                tokens[missingTokenIndex].category == WordCategory.GRAMMAR_MARKER
        )
        require(tokens[missingTokenIndex].japanese == answer)
        require(answer in choices)
        require(choices.size == 6 && choices.distinct().size == 6)
        require(explanation.isNotBlank())
        require(translation.isNotBlank())
    }
}
