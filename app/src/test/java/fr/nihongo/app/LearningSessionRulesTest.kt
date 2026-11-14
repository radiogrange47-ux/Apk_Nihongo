package fr.nihongo.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningSessionRulesTest {
    @Test
    fun completedQuestionsAdvanceSeriesAndSwitchGamesAtTarget() {
        val firstQuestion = LearningSessionRules.afterQuestion(
            questionsCompleted = 0,
            gameIndex = 0,
            gameCount = 3,
            target = 2
        )
        assertEquals(1, firstQuestion.questionsCompleted)
        assertEquals(0, firstQuestion.gameIndex)
        assertFalse(firstQuestion.advanceToNextGame)

        val secondQuestion = LearningSessionRules.afterQuestion(
            questionsCompleted = firstQuestion.questionsCompleted,
            gameIndex = firstQuestion.gameIndex,
            gameCount = 3,
            target = 2
        )
        assertEquals(0, secondQuestion.questionsCompleted)
        assertEquals(1, secondQuestion.gameIndex)
        assertTrue(secondQuestion.advanceToNextGame)
    }

    @Test
    fun singleGameCyclesAfterConfiguredNumberOfQuestions() {
        val firstQuestion = LearningSessionRules.afterQuestion(
            questionsCompleted = 3,
            gameIndex = 0,
            gameCount = 1,
            target = 5
        )
        assertEquals(4, firstQuestion.questionsCompleted)
        assertEquals(0, firstQuestion.gameIndex)
        assertFalse(firstQuestion.advanceToNextGame)

        val singleGame = LearningSessionRules.afterQuestion(
            questionsCompleted = 4,
            gameIndex = 0,
            gameCount = 1,
            target = 5
        )
        assertEquals(0, singleGame.gameIndex)
        assertEquals(0, singleGame.questionsCompleted)
        assertTrue(singleGame.advanceToNextGame)
    }

}
