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

    private companion object {
        const val PREFERENCES_NAME = "learning_settings"
        const val EXTRA_ATTEMPTS_KEY = "extra_attempts"
        const val DEFAULT_EXTRA_ATTEMPTS = 1
        const val MIN_EXTRA_ATTEMPTS = 0
        const val MAX_EXTRA_ATTEMPTS = 5
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
