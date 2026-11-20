package fr.nihongo.app

internal data class SessionProgressUpdate(
    val gameIndex: Int,
    val questionsCompleted: Int,
    val advanceToNextGame: Boolean
)

internal object LearningSessionRules {
    fun allocateQuestionTargets(
        games: List<LearningGame>,
        eligibleCounts: Map<LearningGame, Int>,
        questionsPerGame: Int
    ): Map<LearningGame, Int> {
        require(games.distinct().size == games.size)
        require(questionsPerGame > 0)
        games.forEach { require((eligibleCounts[it] ?: 0) >= 0) }

        return games.associateWith { game ->
            minOf(questionsPerGame, eligibleCounts[game] ?: 0)
        }.filterValues { it > 0 }
    }

    fun afterQuestion(
        questionsCompleted: Int,
        gameIndex: Int,
        gameCount: Int,
        target: Int,
    ): SessionProgressUpdate {
        require(gameCount > 0)
        require(gameIndex in 0 until gameCount)
        require(questionsCompleted >= 0)
        require(target > 0)

        val updatedCount = if (questionsCompleted == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            questionsCompleted + 1
        }
        if (updatedCount >= target) {
            return SessionProgressUpdate(
                gameIndex = (gameIndex + 1) % gameCount,
                questionsCompleted = 0,
                advanceToNextGame = true
            )
        }
        return SessionProgressUpdate(
            gameIndex = gameIndex,
            questionsCompleted = updatedCount,
            advanceToNextGame = false
        )
    }
}
