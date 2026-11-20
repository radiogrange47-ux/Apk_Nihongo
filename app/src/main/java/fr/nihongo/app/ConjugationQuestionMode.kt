package fr.nihongo.app

import kotlin.random.Random

internal enum class ConjugationQuestionMode(val title: String, val instruction: String) {
    CHOOSE_ENDING("Choisir", "Sélectionne la terminaison qui complète la phrase."),
    TYPE_ENDING("Écrire", "Retrouve et écris la terminaison japonaise."),
    FRENCH_TO_JAPANESE("Traduire", "Choisis la phrase japonaise qui exprime cette idée.");

    companion object {
        fun forQuestionIndex(index: Int, offset: Int): ConjugationQuestionMode =
            values()[(index + offset) % values().size]
    }
}

internal fun conjugationSentenceChoices(
    question: ConjugationQuestion,
    questions: List<ConjugationQuestion>,
    random: Random = Random.Default
): List<String> {
    val correct = question.completedJapaneseSentence()
    val otherQuestions = questions
        .filter { it.id != question.id }
        .map { it.completedJapaneseSentence() }
    val alternateForms = question.choices.map { question.completedJapaneseSentence(it) }
    val distractors = (otherQuestions + alternateForms)
        .filter { it != correct }
        .distinct()
        .shuffled(random)
        .take(AnswerChoiceRules.DISPLAYED_CHOICE_COUNT - 1)
    return (distractors + correct).distinct().shuffled(random)
}
