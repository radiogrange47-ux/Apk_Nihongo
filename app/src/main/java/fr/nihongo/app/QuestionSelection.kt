package fr.nihongo.app

import kotlin.random.Random

internal object QuestionMasteryRules {
    fun updateCounter(current: Int, wasCorrect: Boolean): Int {
        require(current >= 0)
        return if (wasCorrect) {
            if (current == Int.MAX_VALUE) current else current + 1
        } else {
            (current - 1).coerceAtLeast(0)
        }
    }
}

internal object QuestionSelector {
    private const val NORMAL_WEIGHT = 100

    fun countEligibleQuestions(
        questions: List<LearningQuestion>,
        masteryCounters: Map<String, Int>,
        masteryThreshold: Int,
        masteredQuestionRepeatPercent: Int
    ): Int {
        require(masteryThreshold > 0)
        require(masteredQuestionRepeatPercent in 0..100)
        return if (masteredQuestionRepeatPercent == 0) {
            questions.count { (masteryCounters[it.id] ?: 0) < masteryThreshold }
        } else {
            questions.size
        }
    }

    fun <T : LearningQuestion> draw(
        questions: List<T>,
        masteryCounters: Map<String, Int>,
        masteryThreshold: Int,
        count: Int,
        masteredQuestionRepeatPercent: Int = 5,
        random: Random = Random.Default
    ): List<T> {
        require(questions.isNotEmpty())
        require(masteryThreshold > 0)
        require(count >= 0)
        require(masteredQuestionRepeatPercent in 0..100)

        val weightedQuestions = questions.map { question ->
            question to if ((masteryCounters[question.id] ?: 0) >= masteryThreshold) {
                masteredQuestionRepeatPercent
            } else {
                NORMAL_WEIGHT
            }
        }

        val totalWeight = weightedQuestions.sumOf { it.second }
        require(count == 0 || totalWeight > 0) {
            "Aucune question non maîtrisée ne reste à réviser avec ce réglage."
        }

        return List(count) {
            var ticket = random.nextInt(totalWeight)
            var selected = weightedQuestions.last().first
            for ((question, weight) in weightedQuestions) {
                if (ticket < weight) {
                    selected = question
                    break
                }
                ticket -= weight
            }
            selected
        }
    }

}

internal class QuestionDeck<T : LearningQuestion>(
    private val questions: List<T>,
    private val random: Random = Random.Default
) {
    private val remainingIds = questions.mapTo(linkedSetOf()) { it.id }

    init {
        require(questions.isNotEmpty())
        require(remainingIds.size == questions.size) {
            "Les identifiants des questions doivent être uniques."
        }
    }

    fun draw(
        masteryCounters: Map<String, Int>,
        masteryThreshold: Int,
        masteredQuestionRepeatPercent: Int = 5
    ): T {
        check(remainingIds.isNotEmpty()) {
            "Toutes les questions de cette série ont déjà été posées."
        }
        val remainingQuestions = questions.filter { it.id in remainingIds }
        val selected = QuestionSelector.draw(
            questions = remainingQuestions,
            masteryCounters = masteryCounters,
            masteryThreshold = masteryThreshold,
            count = 1,
            masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
            random = random
        ).single()
        remainingIds.remove(selected.id)
        return selected
    }
}
