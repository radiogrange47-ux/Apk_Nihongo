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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
    val message: String,
    val frenchTranslation: String? = null
)

private val wordColorLegend = listOf(
    WordColorLegend(WordCategory.VOCABULARY, "Vocabulaire", Color(0xFF64B5F6)),
    WordColorLegend(WordCategory.PARTICLE, "Particules", Color(0xFF81C784)),
    WordColorLegend(WordCategory.VERB, "Verbes", Color(0xFFFFB74D)),
    WordColorLegend(WordCategory.GRAMMAR_MARKER, "Grammaire", Color(0xFFCE93D8))
)

@Composable
internal fun ParticleGameScreen(
    questions: List<ParticleExercise>,
    onBack: () -> Unit,
    onAnswer: (LearningQuestion, LearningGame, Boolean) -> String?,
    onQuestionCompleted: (LearningGame) -> Unit,
    extraAttempts: Int,
    progress: ProgressSnapshot,
    masteryThreshold: Int,
    completedQuestions: Int,
    seriesTarget: Int,
    allQuestionsMastered: Boolean
) {
    var questionIndex by remember { mutableIntStateOf(0) }
    var wrongAnswers by remember { mutableIntStateOf(0) }
    var selectedToken by remember { mutableStateOf<PhraseToken?>(null) }
    var feedback by remember { mutableStateOf<AnswerFeedback?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val initialMasteryCounters = progress.questionsFor(LearningGame.PARTICLES).associate {
        it.questionId to it.masteryCounter
    }
    var masteryCounters by remember { mutableStateOf(initialMasteryCounters) }
    val questionDeck = remember(questions) { QuestionDeck(questions) }
    var exercise by remember(questionDeck) {
        mutableStateOf(
            questionDeck.draw(
                masteryCounters = initialMasteryCounters,
                masteryThreshold = masteryThreshold
            )
        )
    }

    fun advanceToNextQuestion() {
        questionIndex++
        wrongAnswers = 0
        onQuestionCompleted(LearningGame.PARTICLES)
        if (questionIndex < seriesTarget) {
            exercise = questionDeck.draw(
                masteryCounters = masteryCounters,
                masteryThreshold = masteryThreshold
            )
        }
    }

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
                                "Question ${questionIndex + 1} / $seriesTarget",
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
                                saveError = onAnswer(exercise, LearningGame.PARTICLES, correct)
                                val updatedCounter = if (saveError == null) {
                                    QuestionMasteryRules.updateCounter(
                                        current = masteryCounters[exercise.id] ?: 0,
                                        wasCorrect = correct
                                    )
                                } else {
                                    masteryCounters[exercise.id] ?: 0
                                }
                                masteryCounters = masteryCounters + (exercise.id to updatedCounter)
                                selectedToken = null

                                if (correct) {
                                    feedback = AnswerFeedback(
                                        tone = FeedbackTone.CORRECT,
                                        message = "✓",
                                        frenchTranslation = exercise.translation
                                    )
                                    advanceToNextQuestion()
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
                                            message = "×  Réponse : ${answerToken.hiragana}",
                                            frenchTranslation = exercise.translation
                                        )
                                    } else {
                                        AnswerFeedback(
                                            tone = FeedbackTone.INCORRECT,
                                            message = "×  Ce n’est pas $choice. Réessayez."
                                        )
                                    }
                                    if (shouldAdvance) {
                                        advanceToNextQuestion()
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
        SessionProgressFooter(
            questionsCompleted = completedQuestions,
            target = seriesTarget,
            allQuestionsMastered = allQuestionsMastered
        )
    }

    selectedToken?.let { token ->
        Dialog(onDismissRequest = { selectedToken = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF151515),
                border = BorderStroke(1.dp, Color(0xFF393939)),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = token.japanese,
                        color = wordColorLegend.first { it.category == token.category }.color,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(token.hiragana, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        token.french,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
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
            text = buildAnnotatedString {
                append(feedback.message)
                feedback.frenchTranslation?.let { translation ->
                    append(" ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(translation)
                    }
                }
            },
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
