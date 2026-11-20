package fr.nihongo.app

import kotlin.random.Random

internal object AnswerChoiceRules {
    const val DISPLAYED_CHOICE_COUNT = 4

    fun four(
        choices: List<String>,
        correctAnswer: String,
        random: Random = Random.Default
    ): List<String> {
        val distractors = choices
            .filter { it != correctAnswer }
            .distinct()
            .shuffled(random)
            .take(DISPLAYED_CHOICE_COUNT - 1)
        return (distractors + correctAnswer).shuffled(random)
    }
}
