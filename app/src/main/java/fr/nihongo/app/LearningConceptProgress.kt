package fr.nihongo.app

import java.text.Normalizer

internal data class LearningConceptProgress(
    val total: Int,
    val mastered: Int,
    val attempted: Int
) {
    companion object {
        /**
         * Collapses repeated examples into a learning notion. Lexical items use their Japanese form;
         * grammar items use their bank and topic. Pair and counter modes contribute attempts to
         * the lexical notion they practice, but do not create extra notions of their own.
         */
        fun from(
            catalog: QuestionCatalog,
            progress: ProgressSnapshot,
            masteryThreshold: Int,
            includeAuxiliaryModes: Boolean = true
        ): LearningConceptProgress {
            require(masteryThreshold > 0)

            val pairIdsByMeaning = catalog.pairingQuestions().associate {
                Triple(it.kanji, it.hiragana, it.answer) to it.id
            }
            val concepts = linkedMapOf<ConceptKey, MutableSet<QuestionRef>>()

            fun register(question: LearningQuestion, game: LearningGame) {
                val key = when (game) {
                    LearningGame.PARTICLES -> ConceptKey(
                        kind = "particle",
                        first = question.bank.name,
                        second = normalize(question.topic),
                        third = normalize((question as ParticleExercise).answer)
                    )
                    LearningGame.VOCABULARY,
                    LearningGame.VOCABULARY_AUDIO,
                    LearningGame.VOCABULARY_FRENCH_TO_JAPANESE,
                    LearningGame.VOCABULARY_WRITE,
                    LearningGame.VERBS -> {
                        val vocabulary = question as VocabularyQuestion
                        ConceptKey(
                            kind = "word",
                            first = normalize(vocabulary.kanji),
                            second = normalize(vocabulary.hiragana)
                        )
                    }
                    LearningGame.CONJUGATION -> {
                        val conjugation = question as ConjugationQuestion
                        ConceptKey(
                            kind = "conjugation",
                            first = question.bank.name,
                            second = normalize(conjugation.topic)
                        )
                    }
                    LearningGame.PAIRS, LearningGame.COUNTER_CHALLENGE -> return
                }

                val refs = concepts.getOrPut(key) { linkedSetOf() }
                refs += QuestionRef(game, question.id)
                if (question is VocabularyQuestion && includeAuxiliaryModes) {
                    pairIdsByMeaning[Triple(question.kanji, question.hiragana, question.answer)]
                        ?.let { refs += QuestionRef(LearningGame.PAIRS, it) }
                    if (question.bank == QuestionBank.COUNTERS) {
                        refs += QuestionRef(LearningGame.COUNTER_CHALLENGE, "counter-${question.id}")
                    }
                }
            }

            catalog.particles.forEach { register(it, LearningGame.PARTICLES) }
            catalog.vocabulary.forEach { question ->
                VOCABULARY_FORMAT_GAMES.forEach { game -> register(question, game) }
            }
            catalog.verbs.forEach { question ->
                VOCABULARY_FORMAT_GAMES.forEach { game -> register(question, game) }
                register(question, LearningGame.VERBS)
            }
            catalog.conjugation.forEach { register(it, LearningGame.CONJUGATION) }

            val progressByQuestion = progress.questions.associateBy { QuestionRef(it.game, it.questionId) }
            val statuses = concepts.values.map { refs ->
                refs.mapNotNull { ref -> progressByQuestion[ref] }
            }
            return LearningConceptProgress(
                total = statuses.size,
                mastered = statuses.count { rows -> rows.any { it.masteryCounter >= masteryThreshold } },
                attempted = statuses.count { rows -> rows.any { it.attempts > 0 } }
            )
        }

        private fun normalize(value: String): String =
            Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
    }
}

private data class ConceptKey(
    val kind: String,
    val first: String,
    val second: String,
    val third: String = ""
)

private data class QuestionRef(val game: LearningGame, val id: String)
