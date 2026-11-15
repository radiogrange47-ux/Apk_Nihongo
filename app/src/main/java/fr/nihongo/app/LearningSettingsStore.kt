package fr.nihongo.app

import android.content.Context

internal class LearningSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun extraAttempts(): Int = preferences.getInt(EXTRA_ATTEMPTS_KEY, DEFAULT_EXTRA_ATTEMPTS)
        .coerceIn(MIN_EXTRA_ATTEMPTS, MAX_EXTRA_ATTEMPTS)

    fun setExtraAttempts(value: Int) {
        require(value in MIN_EXTRA_ATTEMPTS..MAX_EXTRA_ATTEMPTS)
        check(preferences.edit().putInt(EXTRA_ATTEMPTS_KEY, value).commit()) {
            "Impossible d’enregistrer le réglage des tentatives."
        }
    }

    fun questionMasteryThreshold(): Int =
        preferences.getInt(MASTERY_THRESHOLD_KEY, DEFAULT_MASTERY_THRESHOLD)
            .coerceAtLeast(MIN_MASTERY_THRESHOLD)

    fun setQuestionMasteryThreshold(value: Int) {
        require(value >= MIN_MASTERY_THRESHOLD)
        check(preferences.edit().putInt(MASTERY_THRESHOLD_KEY, value).commit()) {
            "Impossible d’enregistrer le seuil de maîtrise des questions."
        }
    }

    fun masteredQuestionRepeatPercent(): Int =
        preferences.getInt(MASTERED_REPEAT_PERCENT_KEY, DEFAULT_MASTERED_REPEAT_PERCENT)
            .coerceIn(MIN_MASTERED_REPEAT_PERCENT, MAX_MASTERED_REPEAT_PERCENT)

    fun setMasteredQuestionRepeatPercent(value: Int) {
        require(value in MIN_MASTERED_REPEAT_PERCENT..MAX_MASTERED_REPEAT_PERCENT)
        check(preferences.edit().putInt(MASTERED_REPEAT_PERCENT_KEY, value).commit()) {
            "Impossible d’enregistrer la fréquence de réapparition des questions maîtrisées."
        }
    }

    fun sessionQuestionCount(): Int =
        preferences.getInt(
            SESSION_QUESTION_COUNT_KEY,
            preferences.getInt(LEGACY_SESSION_STREAK_KEY, DEFAULT_SESSION_QUESTION_COUNT)
        ).coerceAtLeast(MIN_SESSION_QUESTION_COUNT)

    fun setSessionQuestionCount(value: Int) {
        require(value >= MIN_SESSION_QUESTION_COUNT)
        check(
            preferences.edit()
                .putInt(SESSION_QUESTION_COUNT_KEY, value)
                .remove(LEGACY_SESSION_STREAK_KEY)
                .commit()
        ) {
            "Impossible d’enregistrer le nombre de questions par mini-jeu."
        }
    }

    fun selectedQuestionBanks(): Set<QuestionBank> {
        if (!preferences.contains(QUESTION_BANKS_KEY)) return setOf(QuestionBank.JLPT5)
        return preferences.getString(QUESTION_BANKS_KEY, "").orEmpty().split(",").mapNotNull { name ->
            QuestionBank.values().firstOrNull { it.name == name }
        }.toSet()
    }

    fun setSelectedQuestionBanks(banks: Set<QuestionBank>) {
        check(preferences.edit().putString(QUESTION_BANKS_KEY, banks.joinToString(",") { it.name }).commit()) {
            "Impossible d’enregistrer les banques sélectionnées."
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "learning_settings"
        const val EXTRA_ATTEMPTS_KEY = "extra_attempts"
        const val MASTERY_THRESHOLD_KEY = "question_mastery_threshold"
        const val MASTERED_REPEAT_PERCENT_KEY = "mastered_question_repeat_percent"
        const val SESSION_QUESTION_COUNT_KEY = "session_question_count"
        const val QUESTION_BANKS_KEY = "selected_question_banks"
        const val LEGACY_SESSION_STREAK_KEY = "session_correct_streak"
        const val DEFAULT_EXTRA_ATTEMPTS = 1
        const val DEFAULT_MASTERY_THRESHOLD = 25
        const val DEFAULT_SESSION_QUESTION_COUNT = 10
        const val MIN_EXTRA_ATTEMPTS = 0
        const val MAX_EXTRA_ATTEMPTS = 5
        const val MIN_MASTERY_THRESHOLD = 1
        const val DEFAULT_MASTERED_REPEAT_PERCENT = 5
        const val MIN_MASTERED_REPEAT_PERCENT = 0
        const val MAX_MASTERED_REPEAT_PERCENT = 100
        const val MIN_SESSION_QUESTION_COUNT = 1
    }
}

internal object ParticleAnswerRules {
    fun shouldAdvanceAfterWrongAnswer(
        previousWrongAnswers: Int,
        extraAttempts: Int
    ): Boolean {
        require(previousWrongAnswers >= 0)
        require(extraAttempts in 0..5)
        return previousWrongAnswers >= extraAttempts
    }
}
