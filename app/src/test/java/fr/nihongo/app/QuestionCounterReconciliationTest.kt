package fr.nihongo.app

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionCounterReconciliationTest {
    @Test
    fun keepsExistingCountersAddsNewOnesAtZeroAndDropsRemovedQuestions() {
        val actual = QuestionCounterReconciliation.reconcile(
            existingCounters = mapOf(
                "VOCABULARY.old.attempts" to 12,
                "VOCABULARY.kept.attempts" to 7,
                "VOCABULARY.kept.mastery" to 4
            ),
            expectedKeys = setOf(
                "VOCABULARY.kept.attempts",
                "VOCABULARY.kept.mastery",
                "VOCABULARY.new.attempts",
                "VOCABULARY.new.mastery"
            )
        )

        assertEquals(
            mapOf(
                "VOCABULARY.kept.attempts" to 7,
                "VOCABULARY.kept.mastery" to 4,
                "VOCABULARY.new.attempts" to 0,
                "VOCABULARY.new.mastery" to 0
            ),
            actual
        )
    }
}
