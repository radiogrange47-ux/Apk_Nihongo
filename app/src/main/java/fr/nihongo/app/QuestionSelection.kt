package fr.nihongo.app

import kotlin.random.Random

internal object QuestionMasteryRules {
    fun updateCounter(current: Int, wasCorrect: Boolean, threshold: Int = 5): Int {
        require(current >= 0)
        require(threshold > 0)
        val normalizedCurrent = current.coerceAtMost(threshold)
        return if (wasCorrect) {
            (normalizedCurrent + 1).coerceAtMost(threshold)
        } else {
            (normalizedCurrent - 1).coerceAtLeast(0)
        }
    }
}

internal enum class MasteryStage(val label: String) {
    DISCOVER("À découvrir"),
    FIRST_WIN("Découverte"),
    LEARNING("En cours"),
    NEARLY_READY("Presque acquis"),
    MASTERED("Acquise");

    companion object {
        fun from(counter: Int, threshold: Int, attempts: Int = counter): MasteryStage {
            require(threshold > 0)
            if (counter <= 0) return if (attempts > 0) LEARNING else DISCOVER
            if (counter >= threshold) return MASTERED
            if (counter == 1) return FIRST_WIN
            return if (counter < threshold - 1) LEARNING else NEARLY_READY
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

internal object CounterChallengeQuestionRules {
    fun randomizedChoices(
        question: VocabularyQuestion,
        questions: List<VocabularyQuestion>,
        random: Random = Random.Default
    ): List<String> {
        val distractors = questions
            .filter { it.topic == question.topic && it.id != question.id }
            .map { it.answer }
            .distinct()
            .shuffled(random)
            .take(5)
        return (distractors + question.answer).distinct().shuffled(random)
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
