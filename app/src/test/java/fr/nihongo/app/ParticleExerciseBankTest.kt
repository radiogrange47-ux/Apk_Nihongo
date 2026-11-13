package fr.nihongo.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticleExerciseBankTest {
    @Test
    fun everyExerciseHasOneValidMissingParticleAndFrenchTranslations() {
        assertEquals(10, ParticleExerciseBank.exercises.size)
        assertEquals(
            ParticleExerciseBank.exercises.size,
            ParticleExerciseBank.exercises.map { it.id }.distinct().size
        )

        ParticleExerciseBank.exercises.forEach { exercise ->
            assertTrue(exercise.answer in exercise.choices)
            assertTrue(exercise.translation.isNotBlank())
            assertTrue(exercise.explanation.isNotBlank())
            assertTrue(exercise.tokens.all {
                it.romaji.isNotBlank() && it.french.isNotBlank()
            })
        }
    }

    @Test
    fun progressMasteryRequiresEnoughAnswersAndAtLeastEightyPercent() {
        val topic = TopicProgress(topic = "を — objet", attempts = 2, correctAnswers = 2)
        assertFalse(topic.isMastered)
        assertTrue(
            TopicProgress(topic = "を — objet", attempts = 5, correctAnswers = 4).isMastered
        )
        assertFalse(
            TopicProgress(topic = "を — objet", attempts = 5, correctAnswers = 3).isMastered
        )
    }
}
