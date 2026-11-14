package fr.nihongo.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

private data class WordColorLegend(
    val category: WordCategory,
    val label: String,
    val color: Color
)

private enum class FeedbackTone {
    CORRECT,
    INCORRECT
}

private data class AnswerFeedback(
    val tone: FeedbackTone,
    val message: String
)

private val wordColorLegend = listOf(
    WordColorLegend(WordCategory.VOCABULARY, "Vocabulaire", Color(0xFF64B5F6)),
    WordColorLegend(WordCategory.PARTICLE, "Particules", Color(0xFF81C784)),
    WordColorLegend(WordCategory.VERB, "Verbes", Color(0xFFFFB74D)),
    WordColorLegend(WordCategory.GRAMMAR_MARKER, "Grammaire", Color(0xFFCE93D8))
)

@Composable
internal fun ParticleGameScreen(
    onBack: () -> Unit,
    onAnswer: (ParticleExercise, Boolean) -> String?,
    extraAttempts: Int
) {
    var questionIndex by remember { mutableIntStateOf(0) }
    var wrongAnswers by remember { mutableIntStateOf(0) }
    var selectedToken by remember { mutableStateOf<PhraseToken?>(null) }
    var feedback by remember { mutableStateOf<AnswerFeedback?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }

    val exercises = ParticleExerciseBank.exercises

    if (completed) {
        GameSummary(
            correctCount = correctCount,
            questionCount = exercises.size,
            onBack = onBack,
            onPlayAgain = {
                questionIndex = 0
                wrongAnswers = 0
                selectedToken = null
                feedback = null
                saveError = null
                correctCount = 0
                completed = false
            },
            finalFeedback = feedback
        )
        return
    }

    val exercise = exercises[questionIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("‹", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
                Text("Accueil", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = BorderStroke(1.dp, Color(0xFF292929))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                if (feedback == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF080808))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Complète la phrase",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${questionIndex + 1} / ${exercises.size}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    AnswerFeedbackBanner(feedback!!)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    JapanesePhrase(
                        exercise = exercise,
                        onSelectToken = { selectedToken = it }
                    )
                }

                WordColorLegendRow()
            }
        }

        exercise.choices.chunked(2).forEach { rowChoices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowChoices.forEach { choice ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(88.dp)
                            .semantics {
                                contentDescription = "Choisir la particule $choice"
                            }
                            .clickable {
                                val correct = choice == exercise.answer
                                saveError = onAnswer(exercise, correct)
                                selectedToken = null

                                if (correct) {
                                    correctCount++
                                    feedback = AnswerFeedback(
                                        tone = FeedbackTone.CORRECT,
                                        message = "✓  ${exercise.translation}"
                                    )
                                                    if (questionIndex == exercises.lastIndex) {
                                                        completed = true
                                                    } else {
                                                        questionIndex++
                                                        wrongAnswers = 0
                                                    }
                                } else {
                                    val shouldAdvance = ParticleAnswerRules.shouldAdvanceAfterWrongAnswer(
                                        previousWrongAnswers = wrongAnswers,
                                        extraAttempts = extraAttempts
                                    )
                                    wrongAnswers++
                                    feedback = if (shouldAdvance) {
                                        val answerToken = exercise.tokens[exercise.missingTokenIndex]
                                        AnswerFeedback(
                                            tone = FeedbackTone.INCORRECT,
                                            message = "×  ${exercise.answer} = ${answerToken.romaji} · ${exercise.translation}"
                                        )
                                    } else {
                                        AnswerFeedback(
                                            tone = FeedbackTone.INCORRECT,
                                            message = "×  Ce n’est pas $choice. Réessayez."
                                        )
                                    }
                                    if (shouldAdvance) {
                                        if (questionIndex == exercises.lastIndex) {
                                            completed = true
                                        } else {
                                            questionIndex++
                                            wrongAnswers = 0
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                        border = BorderStroke(1.dp, Color(0xFF222222))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (rowChoices.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        saveError?.let { message ->
            Text(
                "Progression non enregistrée : $message",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    selectedToken?.let { token ->
        AlertDialog(
            onDismissRequest = { selectedToken = null },
            title = {
                Text(
                    text = token.japanese,
                    color = wordColorLegend.first { it.category == token.category }.color,
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rōmaji : ${token.romaji}", style = MaterialTheme.typography.titleMedium)
                    Text("Français : ${token.french}", style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedToken = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
private fun WordColorLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        wordColorLegend.forEach { item ->
            Text(
                item.label,
                color = item.color,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnswerFeedbackBanner(feedback: AnswerFeedback) {
    val backgroundColor = when (feedback.tone) {
        FeedbackTone.CORRECT -> Color(0xFF12352A)
        FeedbackTone.INCORRECT -> Color(0xFF3A1717)
    }
    val foregroundColor = when (feedback.tone) {
        FeedbackTone.CORRECT -> Color(0xFF8EE6B5)
        FeedbackTone.INCORRECT -> Color(0xFFFF9999)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, foregroundColor.copy(alpha = 0.45f))
    ) {
        Text(
            text = feedback.message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            color = foregroundColor,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JapanesePhrase(
    exercise: ParticleExercise,
    onSelectToken: (PhraseToken) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp, androidx.compose.ui.Alignment.CenterHorizontally)
    ) {
        exercise.tokens.forEachIndexed { index, token ->
            if (index == exercise.missingTokenIndex) {
                Text(
                    "【 ？ 】",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        contentDescription = "Particule manquante"
                    }
                )
            } else {
                val color = wordColorLegend.first { it.category == token.category }.color
                Text(
                    text = token.japanese,
                    color = color,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onSelectToken(token) }
                        .semantics {
                            contentDescription = "${token.japanese}, ${token.french}"
                        }
                )
            }
        }
    }
}

@Composable
private fun GameSummary(
    correctCount: Int,
    questionCount: Int,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit,
    finalFeedback: AnswerFeedback?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Séance terminée !", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "$correctCount bonne(s) réponse(s) sur $questionCount.",
            style = MaterialTheme.typography.titleLarge
        )
        finalFeedback?.let {
            Spacer(modifier = Modifier.height(16.dp))
            AnswerFeedbackBanner(it)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Rejouer")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour au menu")
        }
    }
}
