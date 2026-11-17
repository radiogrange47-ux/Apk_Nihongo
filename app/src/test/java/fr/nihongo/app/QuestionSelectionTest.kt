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

    @Test
    fun counterChallengeChoicesStayValidAndVaryBetweenDraws() {
        val questions = (0 until 10).map { index ->
            val answer = "réponse-$index"
            VocabularyQuestion(
                id = "counter-$index",
                topic = "Compteurs · animaux",
                kanji = "mot-$index",
                hiragana = "よみ$index",
                answer = answer,
                choices = listOf(answer, "leurre-a", "leurre-b", "leurre-c", "leurre-d", "leurre-e"),
                bank = QuestionBank.COUNTERS
            )
        }

        val target = questions.first()
        val draws = (1..20).map { seed ->
            CounterChallengeQuestionRules.randomizedChoices(target, questions, Random(seed))
        }

        assertTrue(draws.all { it.size == 6 && it.distinct().size == 6 && target.answer in it })
        assertTrue(draws.map { it.toSet() }.distinct().size > 1)
    }

    @Test
    fun pairQuestionIdsStayStableWhenBanksAreFiltered() {
        fun duplicateQuestion(id: String, bank: QuestionBank) = VocabularyQuestion(
            id = id,
            topic = "Eau",
            kanji = "水",
            hiragana = "みず",
            answer = "eau",
            choices = listOf("eau", "feu", "arbre", "livre", "chat", "maison"),
            bank = bank
        )

        val catalog = QuestionCatalog(
            contentVersion = "1.0.0",
            particles = emptyList(),
            vocabulary = listOf(
                duplicateQuestion("jlpt5-water", QuestionBank.JLPT5),
                duplicateQuestion("course-water", QuestionBank.COURSE)
            ),
            verbs = emptyList(),
            conjugation = emptyList()
        )
        val installedId = catalog.pairingQuestions().single().id
        val filteredId = catalog.forBanks(setOf(QuestionBank.COURSE)).pairingQuestions().single().id

        assertEquals(installedId, filteredId)
        assertEquals("pairs-jlpt5-water", catalog.legacyPairingIds()[installedId])
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
