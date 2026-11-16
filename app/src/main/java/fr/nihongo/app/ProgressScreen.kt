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
    masteryThreshold: Int,
    onBack: () -> Unit
) {
    var selectedGameName by rememberSaveable { mutableStateOf("") }
    val selectedGame = LearningGame.values().firstOrNull { it.name == selectedGameName }
    BackHandler(enabled = selectedGame != null) { selectedGameName = "" }

    if (selectedGame == null) {
        ProgressOverview(
            progress = progress,
            masteryThreshold = masteryThreshold,
            onBack = onBack,
            onOpenGame = { selectedGameName = it.name }
        )
    } else {
        GameProgressDetails(
            game = selectedGame,
            progress = progress,
            masteryThreshold = masteryThreshold,
            onBack = { selectedGameName = "" }
        )
    }
}

@Composable
private fun ProgressOverview(
    progress: ProgressSnapshot,
    masteryThreshold: Int,
    onBack: () -> Unit,
    onOpenGame: (LearningGame) -> Unit
) {
    val questionCount = progress.questions.size
    val attemptedCount = progress.questions.count { it.attempts > 0 }
    val masteredCount = progress.questions.count { it.masteryCounter >= masteryThreshold }
    val remainingCount = (questionCount - masteredCount).coerceAtLeast(0)
    val completion = if (questionCount == 0) 0f else masteredCount.toFloat() / questionCount
    val encouragement = when {
        questionCount == 0 -> "Aucune question n’est disponible pour le moment."
        progress.totalAttempts == 0 -> "Ton parcours commence ici : choisis un exercice et lance-toi."
        masteredCount == questionCount -> "Tout est maîtrisé. Tu peux continuer à entretenir tes acquis."
        masteredCount == 0 -> "Bonne lancée : chaque réponse te rapproche de tes premières notions maîtrisées."
        else -> "Tu avances bien. Il te reste $remainingCount notion(s) à consolider."
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
                LinearProgressIndicator(
                    progress = { completion.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressSuccess
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressMetric("$masteredCount / $questionCount", "maîtrisées", Modifier.weight(1f))
                    ProgressMetric("$attemptedCount", "découvertes", Modifier.weight(1f))
                    ProgressMetric("${progress.totalAttempts}", "réponses", Modifier.weight(1f))
                }
                Text(
                    "Il te reste $remainingCount question(s) à maîtriser.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Réussite globale : ${progress.accuracyLabel()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Tes exercices", style = MaterialTheme.typography.titleLarge)
        LearningGame.values().forEach { game ->
            val questions = progress.questionsFor(game)
            val started = questions.count { it.attempts > 0 }
            val mastered = questions.count { it.masteryCounter >= masteryThreshold }
            val remaining = (questions.size - mastered).coerceAtLeast(0)
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
                            if (questions.isEmpty()) 0f else mastered.toFloat() / questions.size
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = ProgressSuccess
                    )
                    Text(
                        "$mastered / ${questions.size} maîtrisées  ·  $remaining à maîtriser  ·  $started découvertes  ·  réussite ${questions.accuracyLabel()}",
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
    masteryThreshold: Int,
    onBack: () -> Unit
) {
    var expandedTopic by rememberSaveable(game.name) { mutableStateOf("") }
    val questions = progress.questionsFor(game)
    val topics = questions.groupBy { it.topic }.map { (name, entries) ->
        TopicDetail(name, entries, masteryThreshold)
    }.sortedWith(
        compareBy<TopicDetail> { it.completionPercent }
            .thenBy { it.accuracyPercent ?: Int.MAX_VALUE }
            .thenBy { it.name }
    )
    val attempted = questions.count { it.attempts > 0 }
    val mastered = questions.count { it.masteryCounter >= masteryThreshold }
    val completion = if (questions.isEmpty()) 0f else mastered.toFloat() / questions.size

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
                    else "$mastered question(s) maîtrisée(s) sur ${questions.size}",
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
                                    "${topic.masteredCount} / ${topic.questions.size} maîtrisées  ·  ${topic.attemptedCount} découvertes",
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
    val attemptedCount: Int
        get() = questions.count { it.attempts > 0 }
    val completionPercent: Int
        get() = if (questions.isEmpty()) 0 else masteredCount * 100 / questions.size
    val accuracyPercent: Int?
        get() {
            val attempts = questions.sumOf { it.attempts }
            return if (attempts == 0) null else questions.sumOf { it.correctAnswers } * 100 / attempts
        }
}

@Composable
private fun QuestionProgressRow(question: QuestionProgress, masteryThreshold: Int) {
    val isMastered = question.masteryCounter >= masteryThreshold
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
            when {
                isMastered -> "Maîtrisée · ${question.attempts} réponses · ${question.accuracyPercent}% de réussite"
                question.attempts == 0 -> "À découvrir · compteur ${question.masteryCounter} / $masteryThreshold"
                else -> "À renforcer · ${question.attempts} réponses · ${question.accuracyPercent}% · compteur ${question.masteryCounter} / $masteryThreshold"
            },
            color = if (isMastered) ProgressSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun ProgressSnapshot.accuracyLabel(): String =
    if (totalAttempts == 0) "—" else "$overallAccuracyPercent %"

private fun LearningGame.progressTitle(): String = when (this) {
    LearningGame.PARTICLES -> "Particules"
    LearningGame.VOCABULARY -> "Vocabulaire"
    LearningGame.VERBS -> "Verbes"
    LearningGame.CONJUGATION -> "Conjugaison"
    LearningGame.PAIRS -> "Paires japonaises"
    LearningGame.COUNTER_CHALLENGE -> "Défi des compteurs"
}
