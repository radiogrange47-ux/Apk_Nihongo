package fr.nihongo.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val ProgressPanel = Color(0xFF151515)
private val ProgressBorder = Color(0xFF292929)
private val ProgressAccent = Color(0xFFE05252)
private val ProgressSuccess = Color(0xFF8EE6B5)

@Composable
internal fun ProgressScreen(
    progress: ProgressSnapshot,
    questionCatalog: QuestionCatalog,
    masteryThreshold: Int,
    onBack: () -> Unit
) {
    var selectedGameName by rememberSaveable { mutableStateOf("") }
    val selectedGame = LearningGame.values().firstOrNull { it.name == selectedGameName }
        ?.takeUnless { it == LearningGame.COUNTER_CHALLENGE || it == LearningGame.VERBS }
    BackHandler(enabled = selectedGame != null) { selectedGameName = "" }

    if (selectedGame == null) {
        ProgressOverview(
            progress = progress,
            questionCatalog = questionCatalog,
            masteryThreshold = masteryThreshold,
            onBack = onBack,
            onOpenGame = { selectedGameName = it.name }
        )
    } else {
        GameProgressDetails(
            game = selectedGame,
            progress = progress,
            questionCatalog = questionCatalog,
            masteryThreshold = masteryThreshold,
            onBack = { selectedGameName = "" }
        )
    }
}

@Composable
private fun ProgressOverview(
    progress: ProgressSnapshot,
    questionCatalog: QuestionCatalog,
    masteryThreshold: Int,
    onBack: () -> Unit,
    onOpenGame: (LearningGame) -> Unit
) {
    val visibleQuestions = progress.visibleQuestions(questionCatalog)
    val exerciseCount = visibleQuestions.size
    val attemptedExerciseCount = visibleQuestions.count { it.attempts > 0 }
    val exerciseStages = visibleQuestions.map { MasteryStage.from(it.masteryCounter, masteryThreshold, it.attempts) }
    val masteredExerciseCount = exerciseStages.count { it == MasteryStage.MASTERED }
    val learningExerciseCount = exerciseStages.count { it == MasteryStage.FIRST_WIN || it == MasteryStage.LEARNING || it == MasteryStage.NEARLY_READY }
    val exerciseCompletion = if (exerciseCount == 0) 0f else exerciseStages.sumOf { it.progressWeight() }.toFloat() / exerciseCount
    val concepts = LearningConceptProgress.from(questionCatalog, progress, masteryThreshold)
    val remainingConceptCount = (concepts.total - concepts.mastered).coerceAtLeast(0)
    val conceptCompletion = concepts.completion
    val encouragement = when {
        concepts.total == 0 -> "Aucune notion n’est disponible pour le moment."
        progress.totalAttempts == 0 -> "Ton parcours commence ici : choisis un exercice et lance-toi."
        concepts.mastered == concepts.total -> "Toutes les notions sont maîtrisées. Tu peux continuer à entretenir tes acquis."
        concepts.mastered == 0 -> "Bonne lancée : chaque réponse te rapproche de tes premières notions maîtrisées."
        else -> "Tu avances bien. Il te reste $remainingConceptCount notion(s) à consolider."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Accueil") }
        Text("Mon parcours", style = MaterialTheme.typography.headlineSmall)

        ProgressCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(encouragement, style = MaterialTheme.typography.titleMedium)
                Text("Notions uniques", style = MaterialTheme.typography.titleSmall)
                LinearProgressIndicator(
                    progress = { conceptCompletion.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressSuccess
                )
                Text(
                    "${concepts.mastered} acquises · ${concepts.nearlyReady} presque acquises · ${concepts.learning + concepts.discovered} en cours · ${concepts.total - concepts.attempted} à découvrir",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Exercices", style = MaterialTheme.typography.titleSmall)
                LinearProgressIndicator(
                    progress = { exerciseCompletion.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "$masteredExerciseCount acquises · $learningExerciseCount en cours · ${exerciseCount - attemptedExerciseCount} à découvrir",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressMetric("${progress.totalAttempts}", "réponses", Modifier.weight(1f))
                    ProgressMetric(progress.accuracyLabel(), "réussite tous exercices", Modifier.weight(1f))
                }
            }
        }

        Text("Tes exercices", style = MaterialTheme.typography.titleLarge)
        LearningGame.values().filterNot {
            it == LearningGame.COUNTER_CHALLENGE || it == LearningGame.VERBS
        }.forEach { game ->
            val questions = visibleQuestions.filter { it.game == game }
            val stages = questions.map { MasteryStage.from(it.masteryCounter, masteryThreshold, it.attempts) }
            val mastered = stages.count { it == MasteryStage.MASTERED }
            val learning = stages.count { it == MasteryStage.FIRST_WIN || it == MasteryStage.LEARNING || it == MasteryStage.NEARLY_READY }
            ProgressCard(
                modifier = Modifier.clickable { onOpenGame(game) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(game.progressTitle(), style = MaterialTheme.typography.titleMedium)
                        Text("›", color = ProgressAccent, style = MaterialTheme.typography.headlineSmall)
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (questions.isEmpty()) 0f else stages.sumOf { it.progressWeight() }.toFloat() / questions.size
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = ProgressSuccess
                    )
                    Text(
                        "$mastered acquises \u00b7 $learning en cours \u00b7 ${questions.size - mastered - learning} \u00e0 d\u00e9couvrir",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GameProgressDetails(
    game: LearningGame,
    progress: ProgressSnapshot,
    questionCatalog: QuestionCatalog,
    masteryThreshold: Int,
    onBack: () -> Unit
) {
    var expandedTopic by rememberSaveable(game.name) { mutableStateOf("") }
    val questions = progress.visibleQuestions(questionCatalog).filter { it.game == game }
    val topics = questions.groupBy { it.topic }.map { (name, entries) ->
        TopicDetail(name, entries, masteryThreshold)
    }.sortedWith(
        compareBy<TopicDetail> { it.completionPercent }
            .thenBy { it.accuracyPercent ?: Int.MAX_VALUE }
            .thenBy { it.name }
    )
    val attempted = questions.count { it.attempts > 0 }
    val stages = questions.map { MasteryStage.from(it.masteryCounter, masteryThreshold, it.attempts) }
    val mastered = stages.count { it == MasteryStage.MASTERED }
    val learning = stages.count { it == MasteryStage.FIRST_WIN || it == MasteryStage.LEARNING || it == MasteryStage.NEARLY_READY }
    val completion = if (questions.isEmpty()) 0f else stages.sumOf { it.progressWeight() }.toFloat() / questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Mon parcours") }
        Text(game.progressTitle(), style = MaterialTheme.typography.headlineSmall)

        ProgressCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (questions.isEmpty()) "Aucune question dans cet exercice."
                    else "$mastered acquises \u00b7 $learning en cours \u00b7 ${questions.size - mastered - learning} \u00e0 d\u00e9couvrir",
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(
                    progress = { completion.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressSuccess
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProgressMetric("$attempted / ${questions.size}", "découvertes")
                    ProgressMetric("${questions.sumOf { it.attempts }}", "réponses")
                    ProgressMetric(questions.accuracyLabel(), "réussite")
                }
            }
        }

        Text("Notions à travailler", style = MaterialTheme.typography.titleLarge)
        if (topics.isEmpty()) {
            ProgressCard {
                Text("Aucune donnée pour cet exercice. Les statistiques apparaîtront après tes premières réponses.")
            }
        } else {
            topics.forEach { topic ->
                val expanded = expandedTopic == topic.name
                ProgressCard(modifier = Modifier.clickable {
                    expandedTopic = if (expanded) "" else topic.name
                }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(topic.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${topic.masteredCount} acquises · ${topic.learningCount} en cours · ${topic.questions.size - topic.masteredCount - topic.learningCount} à découvrir",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(if (expanded) "−" else "+", color = ProgressAccent)
                        }
                        LinearProgressIndicator(
                            progress = { topic.completionPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = ProgressSuccess
                        )
                        if (expanded) {
                            topic.questions.sortedWith(
                                compareBy<QuestionProgress> { it.masteryCounter >= masteryThreshold }
                                    .thenBy { it.masteryCounter }
                                    .thenBy { it.accuracyPercent }
                            ).forEach { question ->
                                QuestionProgressRow(question, masteryThreshold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TopicDetail(
    val name: String,
    val questions: List<QuestionProgress>,
    private val masteryThreshold: Int
) {
    val masteredCount: Int
        get() = questions.count { it.masteryCounter >= masteryThreshold }
    val learningCount: Int
        get() = questions.count { MasteryStage.from(it.masteryCounter, masteryThreshold, it.attempts) in listOf(MasteryStage.FIRST_WIN, MasteryStage.LEARNING, MasteryStage.NEARLY_READY) }
    val completionPercent: Int
        get() = if (questions.isEmpty()) 0 else (questions.sumOf { MasteryStage.from(it.masteryCounter, masteryThreshold, it.attempts).progressWeight() } * 100 / questions.size).toInt()
    val accuracyPercent: Int?
        get() {
            val attempts = questions.sumOf { it.attempts }
            return if (attempts == 0) null else questions.sumOf { it.correctAnswers } * 100 / attempts
        }
}

@Composable
private fun QuestionProgressRow(question: QuestionProgress, masteryThreshold: Int) {
    val stage = MasteryStage.from(question.masteryCounter, masteryThreshold, question.attempts)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            question.prompt,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${stage.label} · ${question.masteryCounter.coerceAtMost(masteryThreshold)} / $masteryThreshold · ${question.attempts} réponses · ${question.accuracyPercent}%",
            color = if (stage == MasteryStage.MASTERED) ProgressSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ProgressCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ProgressPanel),
        border = BorderStroke(1.dp, ProgressBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun ProgressMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

private fun List<QuestionProgress>.accuracyLabel(): String {
    val attempts = sumOf { it.attempts }
    if (attempts == 0) return "—"
    return "${sumOf { it.correctAnswers } * 100 / attempts} %"
}

private fun MasteryStage.progressWeight(): Double = when (this) {
    MasteryStage.DISCOVER -> 0.0
    MasteryStage.FIRST_WIN -> 0.25
    MasteryStage.LEARNING -> 0.5
    MasteryStage.NEARLY_READY -> 0.75
    MasteryStage.MASTERED -> 1.0
}

private fun ProgressSnapshot.accuracyLabel(): String =
    if (totalAttempts == 0) "—" else "$overallAccuracyPercent %"

private fun ProgressSnapshot.visibleQuestions(catalog: QuestionCatalog): List<QuestionProgress> {
    val vocabularyById = catalog.vocabulary.associateBy { it.id }
    val currentVocabularyIds = VOCABULARY_FORMAT_GAMES
        .flatMap { game -> questionsFor(game) }
        .filter { it.attempts > 0 }
        .mapTo(hashSetOf()) { it.questionId }
    val legacyWordHistory = questionsFor(LearningGame.VERBS).map { question ->
        question.copy(game = LearningGame.VOCABULARY)
    }
    val legacyCounterHistory = questionsFor(LearningGame.COUNTER_CHALLENGE).map { question ->
        val questionId = question.questionId.removePrefix("counter-")
        val source = vocabularyById[questionId]
        question.copy(
            game = LearningGame.VOCABULARY,
            questionId = questionId,
            topic = source?.topic ?: question.topic,
            prompt = source?.prompt ?: question.prompt
        )
    }
    return (
        questions.filter {
            it.game != LearningGame.COUNTER_CHALLENGE && it.game != LearningGame.VERBS
        } + legacyWordHistory + legacyCounterHistory
    )
        .groupBy { it.game to it.questionId }
        .values
        .map { rows ->
            val primary = rows.firstOrNull { it.game == LearningGame.VOCABULARY } ?: rows.first()
            val sharedMastery = if (primary.questionId in currentVocabularyIds) {
                rows.filter { it.game in VOCABULARY_FORMAT_GAMES }
                    .maxOfOrNull { it.masteryCounter }
            } else {
                null
            }
            primary.copy(
                attempts = rows.sumOf { it.attempts },
                correctAnswers = rows.sumOf { it.correctAnswers },
                masteryCounter = sharedMastery ?: rows.maxOf { it.masteryCounter }
            )
        }
}

private fun LearningGame.progressTitle(): String = when (this) {
    LearningGame.PARTICLES -> "Particules"
    LearningGame.VOCABULARY -> "Japonais → français"
    LearningGame.VOCABULARY_AUDIO -> "Écoute et traduction"
    LearningGame.VOCABULARY_FRENCH_TO_JAPANESE -> "Français → japonais"
    LearningGame.VOCABULARY_WRITE -> "Écrire en japonais"
    LearningGame.VERBS -> "Verbes"
    LearningGame.CONJUGATION -> "Conjugaison"
    LearningGame.PAIRS -> "Paires"
    LearningGame.COUNTER_CHALLENGE -> "Défi des compteurs"
}
