package fr.nihongo.app

import android.content.Context

internal data class QuestionProgress(
    val game: LearningGame,
    val questionId: String,
    val topic: String,
    val prompt: String,
    val attempts: Int,
    val correctAnswers: Int,
    val masteryCounter: Int
) {
    val accuracyPercent: Int
        get() = if (attempts == 0) 0 else correctAnswers * 100 / attempts

}

internal data class TopicProgress(
    val topic: String,
    val attempts: Int,
    val correctAnswers: Int
) {
    val accuracyPercent: Int
        get() = if (attempts == 0) 0 else correctAnswers * 100 / attempts

    val isMastered: Boolean
        get() = attempts >= MIN_ATTEMPTS_FOR_MASTERY && accuracyPercent >= MASTERY_ACCURACY_PERCENT

    private companion object {
        const val MIN_ATTEMPTS_FOR_MASTERY = 3
        const val MASTERY_ACCURACY_PERCENT = 80
    }
}

internal data class ProgressSnapshot(
    val questions: List<QuestionProgress>,
    val totalAttempts: Int,
    val correctAnswers: Int
) {
    val topics: List<TopicProgress>
        get() = topicsFor(null)

    fun questionsFor(game: LearningGame): List<QuestionProgress> =
        questions.filter { it.game == game }

    fun topicsFor(game: LearningGame?): List<TopicProgress> =
        questions
            .filter { game == null || it.game == game }
            .groupBy { it.topic }
            .map { (topic, entries) ->
                TopicProgress(
                    topic = topic,
                    attempts = entries.sumOf { it.attempts },
                    correctAnswers = entries.sumOf { it.correctAnswers }
                )
            }

    val overallAccuracyPercent: Int
        get() = if (totalAttempts == 0) 0 else correctAnswers * 100 / totalAttempts

    val masteredCount: Int
        get() = topics.count { it.isMastered }
}

internal object QuestionCounterReconciliation {
    fun reconcile(
        existingCounters: Map<String, Int>,
        expectedKeys: Set<String>
    ): Map<String, Int> =
        expectedKeys.associateWith { key -> existingCounters[key] ?: 0 }
}

internal class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recordAnswer(
        question: LearningQuestion,
        game: LearningGame,
        wasCorrect: Boolean,
        catalog: QuestionCatalog
    ): ProgressSnapshot {
        require(catalog.questionsFor(game).any { it.id == question.id }) {
            "Cette question ne fait pas partie du contenu installé."
        }
        val attemptsKey = attemptsKey(question, game)
        val correctKey = correctKey(question, game)
        val masteryKey = masteryKey(question, game)
        val attempts = preferences.getInt(attemptsKey, 0) + 1
        val correctAnswers = preferences.getInt(correctKey, 0) + if (wasCorrect) 1 else 0
        val masteryCounter = QuestionMasteryRules.updateCounter(
            current = preferences.getInt(masteryKey, 0),
            wasCorrect = wasCorrect
        )

        check(
            preferences.edit()
                .putInt(attemptsKey, attempts)
                .putInt(correctKey, correctAnswers)
                .putInt(masteryKey, masteryCounter)
                .commit()
        ) { "Impossible d’enregistrer la réponse sur cet appareil." }

        return snapshot(catalog)
    }

    fun resetMasteryCounters(catalog: QuestionCatalog): ProgressSnapshot {
        val editor = preferences.edit()
        preferences.all.keys
            .filter { it.endsWith(MASTERY_SUFFIX) }
            .forEach(editor::remove)
        check(editor.commit()) { "Impossible de réinitialiser les compteurs des questions." }
        synchronizeQuestions(catalog)
        return snapshot(catalog)
    }

    fun synchronizeQuestions(catalog: QuestionCatalog) {
        val expectedKeys = catalog.allQuestionIds().flatMap { (game, ids) ->
            ids.flatMap { id ->
                listOf(
                    questionKey(id, game, ATTEMPTS_SUFFIX),
                    questionKey(id, game, CORRECT_SUFFIX),
                    questionKey(id, game, MASTERY_SUFFIX)
                )
            }
        }.toSet()
        val questionDataKeys = preferences.all.keys.filter { key ->
            key.endsWith(ATTEMPTS_SUFFIX) ||
                key.endsWith(CORRECT_SUFFIX) ||
                key.endsWith(MASTERY_SUFFIX)
        }
        val existingCounters = questionDataKeys.associateWith { preferences.getInt(it, 0) }
            .toMutableMap()
        catalog.legacyPairingIds().forEach { (stableId, legacyId) ->
            listOf(ATTEMPTS_SUFFIX, CORRECT_SUFFIX, MASTERY_SUFFIX).forEach { suffix ->
                val currentKey = questionKey(stableId, LearningGame.PAIRS, suffix)
                val legacyKey = questionKey(legacyId, LearningGame.PAIRS, suffix)
                if (currentKey !in existingCounters && legacyKey in existingCounters) {
                    existingCounters[currentKey] = existingCounters.getValue(legacyKey)
                }
            }
        }
        val reconciledCounters = QuestionCounterReconciliation.reconcile(
            existingCounters = existingCounters,
            expectedKeys = expectedKeys
        )
        val editor = preferences.edit()
        questionDataKeys.filterNot(expectedKeys::contains).forEach(editor::remove)
        reconciledCounters.forEach { (key, value) -> editor.putInt(key, value) }
        check(editor.commit()) { "Impossible d’ajuster les compteurs au contenu installé." }
    }

    fun snapshot(catalog: QuestionCatalog): ProgressSnapshot {
        val questions = buildList {
            LearningGame.values().forEach { game ->
                addAll(catalog.questionsFor(game).map { question ->
                    questionProgress(question, game)
                })
            }
        }
        return ProgressSnapshot(
            questions = questions,
            totalAttempts = questions.sumOf { it.attempts },
            correctAnswers = questions.sumOf { it.correctAnswers }
        )
    }

    private fun questionProgress(question: LearningQuestion, game: LearningGame) =
        QuestionProgress(
            game = game,
            questionId = question.id,
            topic = question.topic,
            prompt = question.prompt,
            attempts = preferences.getInt(attemptsKey(question, game), 0),
            correctAnswers = preferences.getInt(correctKey(question, game), 0),
            masteryCounter = preferences.getInt(masteryKey(question, game), 0)
        )

    private fun attemptsKey(question: LearningQuestion, game: LearningGame) =
        questionKey(question.id, game, ATTEMPTS_SUFFIX)

    private fun correctKey(question: LearningQuestion, game: LearningGame) =
        questionKey(question.id, game, CORRECT_SUFFIX)

    private fun masteryKey(question: LearningQuestion, game: LearningGame) =
        questionKey(question.id, game, MASTERY_SUFFIX)

    private fun questionKey(questionId: String, game: LearningGame, suffix: String) =
        if (game == LearningGame.PARTICLES) "$questionId.$suffix"
        else "${game.name}.$questionId.$suffix"

    private companion object {
        const val PREFERENCES_NAME = "learning_progress"
        const val MASTERY_SUFFIX = ".mastery"
        const val ATTEMPTS_SUFFIX = ".attempts"
        const val CORRECT_SUFFIX = ".correct"
    }
}
