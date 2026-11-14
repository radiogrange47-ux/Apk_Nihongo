package fr.nihongo.app

internal data class VocabularyQuestion(
    override val id: String,
    override val topic: String,
    val kanji: String,
    val hiragana: String,
    val answer: String,
    val choices: List<String>,
    override val level: String? = null,
    override val source: String? = null,
    override val bank: QuestionBank = QuestionBank.JLPT5
) : LearningQuestion {
    override val prompt: String
        get() = "$kanji\n$hiragana"

    init {
        require(id.isNotBlank())
        require(topic.isNotBlank())
        require(kanji.isNotBlank())
        require(hiragana.isNotBlank())
        require(answer.isNotBlank() && answer in choices)
        require(choices.size == 6 && choices.distinct().size == 6)
        require(choices.all(String::isNotBlank))
    }
}
