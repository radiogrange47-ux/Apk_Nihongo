package fr.nihongo.app

import android.content.Context

internal data class ExerciseProgress(
    val exercise: ParticleExercise,
    val attempts: Int,
    val correctAnswers: Int
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
    val exercises: List<ExerciseProgress>,
    val totalAttempts: Int,
    val correctAnswers: Int
) {
    val topics: List<TopicProgress>
        get() = exercises
            .groupBy { it.exercise.topic }
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

internal class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recordAnswer(exercise: ParticleExercise, wasCorrect: Boolean): ProgressSnapshot {
        val attemptsKey = attemptsKey(exercise)
        val correctKey = correctKey(exercise)
        val attempts = preferences.getInt(attemptsKey, 0) + 1
        val correctAnswers = preferences.getInt(correctKey, 0) + if (wasCorrect) 1 else 0

        check(
            preferences.edit()
                .putInt(attemptsKey, attempts)
                .putInt(correctKey, correctAnswers)
                .commit()
        ) { "Impossible d’enregistrer la réponse sur cet appareil." }

        return snapshot()
    }

    fun snapshot(): ProgressSnapshot {
        val exercises = ParticleExerciseBank.exercises.map { exercise ->
            ExerciseProgress(
                exercise = exercise,
                attempts = preferences.getInt(attemptsKey(exercise), 0),
                correctAnswers = preferences.getInt(correctKey(exercise), 0)
            )
        }
        return ProgressSnapshot(
            exercises = exercises,
            totalAttempts = exercises.sumOf { it.attempts },
            correctAnswers = exercises.sumOf { it.correctAnswers }
        )
    }

    private fun attemptsKey(exercise: ParticleExercise) = "${exercise.id}.attempts"
    private fun correctKey(exercise: ParticleExercise) = "${exercise.id}.correct"

    private companion object {
        const val PREFERENCES_NAME = "learning_progress"
    }
}
