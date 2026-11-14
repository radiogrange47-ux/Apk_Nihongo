package fr.nihongo.app

internal data class ConjugationQuestion(
    override val id: String,
    override val topic: String,
    val contextFrench: String,
    val tokens: List<PhraseToken>,
    val verbTokenIndex: Int,
    val punctuation: String,
    val answerEnding: String,
    val choices: List<String>,
    val translation: String,
    val explanation: String,
    override val level: String? = null,
    override val source: String? = null,
    override val bank: QuestionBank = QuestionBank.JLPT5
) : LearningQuestion {
    override val prompt: String
        get() = tokens.mapIndexed { index, token ->
            if (index == verbTokenIndex) "${token.japanese}..." else token.japanese
        }.joinToString("") + punctuation

    init {
        require(id.isNotBlank())
        require(topic.isNotBlank())
        require(verbTokenIndex in tokens.indices)
        require(tokens[verbTokenIndex].category == WordCategory.VERB)
        require(answerEnding in choices)
        require(choices.size == 6 && choices.distinct().size == 6)
        require(contextFrench.isNotBlank())
        require(translation.isNotBlank())
        require(explanation.isNotBlank())
    }
}
