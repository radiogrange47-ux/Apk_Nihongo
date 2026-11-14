package fr.nihongo.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun VocabularyGameScreen(
    game: LearningGame,
    questions: List<VocabularyQuestion>,
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
    var masteryCounters by remember {
        mutableStateOf(
            progress.questionsFor(game)
                .associate { it.questionId to it.masteryCounter }
        )
    }
    var questionIndex by remember { mutableIntStateOf(0) }
    var wrongAnswers by remember { mutableIntStateOf(0) }
    val questionDeck = remember(questions) { QuestionDeck(questions) }
    var question by remember(questionDeck) {
        mutableStateOf(
            questionDeck.draw(
                masteryCounters = masteryCounters,
                masteryThreshold = masteryThreshold
            )
        )
    }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackTranslation by remember { mutableStateOf<String?>(null) }
    var feedbackCorrect by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    fun advance() {
        questionIndex++
        wrongAnswers = 0
        onQuestionCompleted(game)
        if (questionIndex < seriesTarget) {
            question = questionDeck.draw(
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
        TextButton(onClick = onBack) {
            Text("‹", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
            Text("Accueil", color = MaterialTheme.colorScheme.onSurface)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = BorderStroke(1.dp, Color(0xFF292929))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(game.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Question ${questionIndex + 1} / $seriesTarget",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (feedback != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (feedbackCorrect) Color(0xFF12352A) else Color(0xFF3A1717)
                        )
                    ) {
                        Text(
                            buildAnnotatedString {
                                append(feedback.orEmpty())
                                feedbackTranslation?.let { translation ->
                                    append(translation)
                                    addStyle(
                                        SpanStyle(fontStyle = FontStyle.Italic),
                                        length - translation.length,
                                        length
                                    )
                                }
                            },
                            modifier = Modifier.padding(14.dp),
                            color = if (feedbackCorrect) Color(0xFF8EE6B5) else Color(0xFFFF9999),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        question.kanji,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        question.hiragana,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        question.choices.chunked(2).forEach { choices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                choices.forEach { choice ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable {
                                val correct = choice == question.answer
                                saveError = onAnswer(question, game, correct)
                                if (saveError == null) {
                                    masteryCounters = masteryCounters + (
                                        question.id to QuestionMasteryRules.updateCounter(
                                            current = masteryCounters[question.id] ?: 0,
                                            wasCorrect = correct
                                        )
                                    )
                                }
                                if (correct) {
                                    feedbackCorrect = true
                                    feedback = "✓  "
                                    feedbackTranslation = question.answer
                                    advance()
                                } else {
                                    feedbackCorrect = false
                                    val shouldAdvance = ParticleAnswerRules.shouldAdvanceAfterWrongAnswer(
                                        previousWrongAnswers = wrongAnswers,
                                        extraAttempts = extraAttempts
                                    )
                                    wrongAnswers++
                                    feedback = if (shouldAdvance) {
                                        "Réponse : "
                                    } else {
                                        "Ce n’est pas « $choice ». Réessaie."
                                    }
                                    feedbackTranslation = if (shouldAdvance) question.answer else null
                                    if (shouldAdvance) advance()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
                        border = BorderStroke(1.dp, Color(0xFF292929))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                choice,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
                if (choices.size == 1) Spacer(modifier = Modifier.weight(1f))
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

}
