package fr.nihongo.app

import java.text.Normalizer

/** A small rolling set of concepts shared by the selected mini-games. */
internal data class ActiveLearningPool(
    val activeNotionCount: Int,
    val remainingNotionCount: Int,
    val activeQuestionsByGame: Map<LearningGame, List<LearningQuestion>>,
    val reviewQuestionsByGame: Map<LearningGame, List<LearningQuestion>>
) {
    fun activeQuestionsFor(game: LearningGame): List<LearningQuestion> =
        activeQuestionsByGame[game].orEmpty()

    fun questionsFor(game: LearningGame): List<LearningQuestion> =
        activeQuestionsFor(game) + reviewQuestionsByGame[game].orEmpty()

    companion object {
        const val MAX_ACTIVE_NOTIONS = 15

        fun build(
            catalog: QuestionCatalog,
            games: List<LearningGame>,
            progress: ProgressSnapshot,
            masteryThreshold: Int,
            masteredQuestionRepeatPercent: Int
        ): ActiveLearningPool {
            require(masteryThreshold > 0)
            require(masteredQuestionRepeatPercent in 0..100)

            val groups = linkedMapOf<String, MutableList<Pair<LearningGame, LearningQuestion>>>()
            games.distinct().forEach { game ->
                catalog.questionsFor(game).forEach { question ->
                    val notionId = notionId(game, question)
                    groups.getOrPut(notionId) { mutableListOf() } += (game to question)
                }
            }

            val vocabularyCounters = progress.vocabularyMasteryCounters()
            val countersByGame = games.associateWith { game ->
                if (game in VOCABULARY_FORMAT_GAMES || game == LearningGame.VERBS) {
                    vocabularyCounters
                } else {
                    progress.questionsFor(game).associate { it.questionId to it.masteryCounter }
                }
            }
            fun isMastered(questions: List<Pair<LearningGame, LearningQuestion>>): Boolean =
                questions.any { (game, question) ->
                    (countersByGame[game]?.get(question.id) ?: 0) >= masteryThreshold
                }

            val seed = buildString {
                append(games.map { it.name }.sorted().joinToString(","))
                append('|')
                append((catalog.particles.map { it.bank } + catalog.vocabulary.map { it.bank } +
                    catalog.verbs.map { it.bank } + catalog.conjugation.map { it.bank })
                    .distinct().map { it.name }.sorted().joinToString(","))
            }.hashCode()
            val orderedGroups = groups.entries.sortedWith(
                compareBy<Map.Entry<String, MutableList<Pair<LearningGame, LearningQuestion>>>> {
                    ("$seed:${it.key}").hashCode()
                }.thenBy { it.key }
            )
            val unfinished = orderedGroups.filterNot { isMastered(it.value) }
            val active = unfinished.take(MAX_ACTIVE_NOTIONS)
            val completed = orderedGroups.filter { isMastered(it.value) }
            val review = if (masteredQuestionRepeatPercent == 0) {
                emptyList()
            } else {
                val rotation = progress.totalAttempts / (MAX_ACTIVE_NOTIONS * 10)
                if (completed.isEmpty()) emptyList() else {
                    List(minOf(MAX_ACTIVE_NOTIONS, completed.size)) { index ->
                        completed[(rotation + index) % completed.size]
                    }
                }
            }

            fun questionsByGame(
                notionGroups: List<Map.Entry<String, MutableList<Pair<LearningGame, LearningQuestion>>>>,
                masteredOnly: Boolean = false
            ): Map<LearningGame, List<LearningQuestion>> =
                notionGroups.flatMap { it.value }
                    .filter { (game, question) ->
                        !masteredOnly || (countersByGame[game]?.get(question.id) ?: 0) >= masteryThreshold
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, questions) -> questions.distinctBy { it.id } }

            return ActiveLearningPool(
                activeNotionCount = active.size,
                remainingNotionCount = unfinished.size,
                activeQuestionsByGame = questionsByGame(active),
                reviewQuestionsByGame = questionsByGame(review, masteredOnly = true)
            )
        }

        private fun notionId(game: LearningGame, question: LearningQuestion): String = when (game) {
            LearningGame.PARTICLES -> {
                val particle = question as ParticleExercise
                listOf("particle", question.bank.name, normalize(question.topic), normalize(particle.answer))
                    .joinToString("\u0000")
            }
            LearningGame.CONJUGATION -> {
                listOf("conjugation", question.bank.name, normalize(question.topic)).joinToString("\u0000")
            }
            LearningGame.VOCABULARY,
            LearningGame.VOCABULARY_AUDIO,
            LearningGame.VOCABULARY_FRENCH_TO_JAPANESE,
            LearningGame.VOCABULARY_WRITE,
            LearningGame.VERBS,
            LearningGame.PAIRS,
            LearningGame.COUNTER_CHALLENGE -> {
                val vocabulary = question as VocabularyQuestion
                val kanji = if (game == LearningGame.COUNTER_CHALLENGE) vocabulary.answer else vocabulary.kanji
                listOf("word", normalize(kanji), normalize(vocabulary.hiragana)).joinToString("\u0000")
            }
        }

        private fun normalize(value: String): String =
            Normalizer.normalize(value.trim(), Normalizer.Form.NFC)
    }
}
