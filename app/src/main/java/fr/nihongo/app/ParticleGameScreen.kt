package fr.nihongo.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

internal enum class FeedbackTone {
    CORRECT,
    INCORRECT
}
internal data class AnswerFeedback(
    val tone: FeedbackTone,
    val message: String,
    val frenchTranslation: String? = null,
    val pedagogicalHint: String? = null,
    val kanjiBreakdown: String? = null
)

private val wordColorLegend = listOf(
    WordColorLegend(WordCategory.VOCABULARY, "Vocabulaire", Color(0xFF64B5F6)),
    WordColorLegend(WordCategory.PARTICLE, "Particules", Color(0xFF81C784)),
    WordColorLegend(WordCategory.VERB, "Verbes", Color(0xFFFFB74D)),
    WordColorLegend(WordCategory.GRAMMAR_MARKER, "Grammaire", Color(0xFFCE93D8)),
    WordColorLegend(WordCategory.ADJECTIVE, "Adjectifs", Color(0xFFFF8A80))
)

@Composable
internal fun ParticleGameScreen(
    questions: List<ParticleExercise>,
    questionSpeech: JapaneseQuestionSpeech,
    onBack: () -> Unit,
    onAnswer: (LearningQuestion, LearningGame, Boolean) -> String?,
    onQuestionCompleted: (LearningGame) -> Unit,
    extraAttempts: Int,
    progress: ProgressSnapshot,
    masteryThreshold: Int,
    masteredQuestionRepeatPercent: Int = 5,
    completedQuestions: Int,
    blockQuestionsCompleted: Int,
    blockTarget: Int,
    seriesTarget: Int,
    allQuestionsMastered: Boolean,
    carryoverAnswer: AnswerFeedback?,
    carryoverAnswerEvent: Int
) {
    var questionIndex by remember { mutableIntStateOf(0) }
    var wrongAnswers by remember { mutableIntStateOf(0) }
    var selectedToken by remember { mutableStateOf<PhraseToken?>(null) }
    var feedback by remember { mutableStateOf<AnswerFeedback?>(null) }
    var feedbackEvent by remember { mutableIntStateOf(0) }
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
                masteryThreshold = masteryThreshold,
                masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
            )
        )
    }
    val displayedChoices = remember(exercise.id) {
        AnswerChoiceRules.four(exercise.choices, exercise.answer)
    }
    val spokenQuestionParts = remember(exercise.id) {
        val beforeGap = exercise.tokens.take(exercise.missingTokenIndex)
            .joinToString(separator = "") { it.hiragana }
        val afterGap = exercise.tokens.drop(exercise.missingTokenIndex + 1)
            .joinToString(separator = "") { it.hiragana }
        beforeGap to afterGap
    }
    LaunchedEffect(exercise.id) {
        questionSpeech.speakQuestionWithGap(spokenQuestionParts.first, spokenQuestionParts.second)
    }

    fun advanceToNextQuestion() {
        questionIndex++
        wrongAnswers = 0
        onQuestionCompleted(LearningGame.PARTICLES)
        if (questionIndex < seriesTarget) {
            exercise = questionDeck.draw(
                masteryCounters = masteryCounters,
                masteryThreshold = masteryThreshold,
                masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
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
            QuestionSpeechButton(questionSpeech, spokenQuestionParts.first, spokenQuestionParts.second)
        }
        carryoverAnswer?.let { AnswerFeedbackBanner(it, carryoverAnswerEvent) }

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
                    AnswerFeedbackBanner(feedback!!, feedbackEvent)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    JapanesePhrase(
                        exercise = exercise,
                        questionSpeech = questionSpeech,
                        onSelectToken = { selectedToken = it }
                    )
                }

                WordColorLegendRow()
            }
        }

        displayedChoices.chunked(2).forEach { rowChoices ->
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
                                questionSpeech.speakQuestion(choice)
                                val correct = choice == exercise.answer
                                saveError = onAnswer(exercise, LearningGame.PARTICLES, correct)
                                if (saveError != null) return@clickable
                                feedbackEvent++
                                val updatedCounter = QuestionMasteryRules.updateCounter(
                                    current = masteryCounters[exercise.id] ?: 0,
                                    wasCorrect = correct
                                )
                                masteryCounters = masteryCounters + (exercise.id to updatedCounter)
                                selectedToken = null

                                if (correct) {
                                    feedback = AnswerFeedback(
                                        tone = FeedbackTone.CORRECT,
                                        message = "✓",
                                        frenchTranslation = exercise.translation,
                                        pedagogicalHint = concisePedagogicalHint(exercise.explanation),
                                        kanjiBreakdown = phraseKanjiBreakdown(exercise.tokens)
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
                                            frenchTranslation = exercise.translation,
                                            pedagogicalHint = concisePedagogicalHint(exercise.explanation),
                                            kanjiBreakdown = phraseKanjiBreakdown(exercise.tokens)
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
            questionsCompleted = blockQuestionsCompleted,
            target = blockTarget,
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
internal fun AnswerFeedbackBanner(feedback: AnswerFeedback, eventKey: Int) {
    val scale = remember { Animatable(1f) }
    val horizontalShift = remember { Animatable(0f) }
    LaunchedEffect(eventKey, feedback.tone) {
        if (eventKey > 0) {
            if (feedback.tone == FeedbackTone.CORRECT) {
                horizontalShift.snapTo(0f)
                scale.snapTo(0.96f)
                scale.animateTo(1.035f, tween(110))
                scale.animateTo(1f, tween(150))
            } else {
                scale.snapTo(1f)
                horizontalShift.snapTo(0f)
                repeat(2) {
                    horizontalShift.animateTo(7f, tween(38))
                    horizontalShift.animateTo(-7f, tween(38))
                }
                horizontalShift.animateTo(0f, tween(42))
            }
        }
    }
    val backgroundColor = when (feedback.tone) {
        FeedbackTone.CORRECT -> Color(0xFF12352A)
        FeedbackTone.INCORRECT -> Color(0xFF3A1717)
    }
    val foregroundColor = when (feedback.tone) {
        FeedbackTone.CORRECT -> Color(0xFF8EE6B5)
        FeedbackTone.INCORRECT -> Color(0xFFFF9999)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .offset(x = horizontalShift.value.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, foregroundColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
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
                color = foregroundColor,
                style = MaterialTheme.typography.titleMedium
            )
            feedback.kanjiBreakdown?.let { breakdown ->
                Text(
                    "Kanji : $breakdown",
                    color = foregroundColor.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            feedback.pedagogicalHint?.let { hint ->
                Text(
                    hint,
                    color = foregroundColor.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JapanesePhrase(
    exercise: ParticleExercise,
    questionSpeech: JapaneseQuestionSpeech,
    onSelectToken: (PhraseToken) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp, androidx.compose.ui.Alignment.CenterHorizontally)
    ) {
        exercise.tokens.forEachIndexed { index, token ->
            if (index == exercise.missingTokenIndex) {
                Text(
                    "...",
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
                        .clickable {
                            questionSpeech.speakQuestion(token.hiragana)
                            onSelectToken(token)
                        }
                        .semantics {
                            contentDescription = "${token.japanese}, ${token.french}"
                        }
                )
            }
        }
    }
}
