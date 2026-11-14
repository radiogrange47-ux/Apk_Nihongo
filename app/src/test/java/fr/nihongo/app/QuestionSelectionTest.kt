package fr.nihongo.app

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionSelectionTest {
    @Test
    fun correctAnswersIncreaseAndWrongAnswersDecreaseCounterWithoutGoingBelowZero() {
        assertEquals(1, QuestionMasteryRules.updateCounter(current = 0, wasCorrect = true))
        assertEquals(1, QuestionMasteryRules.updateCounter(current = 2, wasCorrect = false))
        assertEquals(0, QuestionMasteryRules.updateCounter(current = 0, wasCorrect = false))
    }

    @Test
    fun masteredQuestionsAreSelectedMuchLessOften() {
        val mastered = question("mastered")
        val unmastered = question("unmastered")
        val selected = QuestionSelector.draw(
            questions = listOf(mastered, unmastered),
            masteryCounters = mapOf(mastered.id to 25, unmastered.id to 0),
            masteryThreshold = 25,
            count = 20_000,
            random = Random(31)
        )

        assertTrue(selected.count { it.id == mastered.id } < 1_200)
        assertTrue(selected.any { it.id == mastered.id })
    }

    @Test
    fun questionDeckExhaustsWithoutRepeating() {
        val deck = QuestionDeck(listOf(question("first"), question("second")), Random(31))

        val drawn = listOf(
            deck.draw(emptyMap(), masteryThreshold = 25).id,
            deck.draw(emptyMap(), masteryThreshold = 25).id
        )

        assertEquals(2, drawn.distinct().size)
        assertTrue(runCatching { deck.draw(emptyMap(), masteryThreshold = 25) }.isFailure)
    }

    private fun question(id: String) = ParticleExercise(
        id = id,
        topic = "Particule",
        tokens = listOf(PhraseToken("を", "を", "marque l’objet", WordCategory.PARTICLE)),
        missingTokenIndex = 0,
        choices = listOf("を", "は", "が", "に", "で", "へ"),
        answer = "を",
        explanation = "Particule objet.",
        translation = "Traduction."
    )
}
