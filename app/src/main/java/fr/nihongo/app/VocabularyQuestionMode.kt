package fr.nihongo.app

import kotlin.random.Random

internal enum class VocabularyQuestionMode(val instruction: String) {
    AUDIO_TO_FRENCH("Écoute le mot japonais et choisis sa traduction."),
    JAPANESE_TO_FRENCH("Choisis la traduction française."),
    FRENCH_TO_JAPANESE("Choisis le mot japonais correspondant."),
    TYPE_JAPANESE("Écris le mot japonais en kanji ou en hiragana.");

    companion object {
        fun forGame(game: LearningGame): VocabularyQuestionMode = when (game) {
            LearningGame.VOCABULARY,
            LearningGame.VERBS -> JAPANESE_TO_FRENCH
            LearningGame.VOCABULARY_AUDIO -> AUDIO_TO_FRENCH
            LearningGame.VOCABULARY_FRENCH_TO_JAPANESE -> FRENCH_TO_JAPANESE
            LearningGame.VOCABULARY_WRITE -> TYPE_JAPANESE
            else -> error("$game n’est pas un jeu de vocabulaire.")
        }
    }
}

internal fun japaneseAnswerChoices(
    question: VocabularyQuestion,
    questions: List<VocabularyQuestion>,
    random: Random = Random.Default
): List<String> {
    val sameTopic = questions.filter {
        it.id != question.id && it.topic == question.topic && it.bank == question.bank
    }
    val sameBank = questions.filter { it.id != question.id && it.bank == question.bank }
    val candidatePool = if (question.bank == QuestionBank.COUNTERS) {
        sameTopic + sameBank
    } else {
        sameTopic + sameBank + questions.filter { it.id != question.id }
    }
    val candidates = candidatePool
        .map { it.kanji }
        .filter { it != question.kanji }
        .distinct()
        .shuffled(random)
        .take(5)
    return (candidates + question.kanji).distinct().shuffled(random)
}

internal fun counterTranslationChoices(
    question: VocabularyQuestion,
    questions: List<VocabularyQuestion>,
    random: Random = Random.Default
): List<String> {
    val otherCounterMeanings = questions
        .filter { it.id != question.id && it.bank == QuestionBank.COUNTERS }
        .map { it.answer }
        .filter { it != question.answer }
        .distinct()
        .shuffled(random)
        .take(5)
    return (otherCounterMeanings + question.answer).distinct().shuffled(random)
}
