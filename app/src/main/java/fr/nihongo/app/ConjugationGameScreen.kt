package fr.nihongo.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
internal fun ConjugationGameScreen(
    questions: List<ConjugationQuestion>,
    questionSpeech: JapaneseQuestionSpeech,
    onBack: () -> Unit,
    onAnswer: (LearningQuestion, LearningGame, Boolean) -> String?,
    onQuestionCompleted: (LearningGame) -> Unit,
    extraAttempts: Int,
    progress: ProgressSnapshot,
    masteryThreshold: Int,
    masteredQuestionRepeatPercent: Int = 5,
    completedQuestions: Int,
    seriesTarget: Int,
    allQuestionsMastered: Boolean
) {
    var masteryCounters by remember {
        mutableStateOf(
            progress.questionsFor(LearningGame.CONJUGATION)
                .associate { it.questionId to it.masteryCounter }
        )
    }
    var questionIndex by remember { mutableIntStateOf(0) }
    var wrongAnswers by remember { mutableIntStateOf(0) }
    var selectedToken by remember { mutableStateOf<PhraseToken?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackCorrect by remember { mutableStateOf(false) }
    var feedbackHint by remember { mutableStateOf<String?>(null) }
    var feedbackEvent by remember { mutableIntStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val questionDeck = remember(questions) { QuestionDeck(questions) }
    var question by remember(questionDeck) {
        mutableStateOf(
            questionDeck.draw(
                masteryCounters = masteryCounters,
                masteryThreshold = masteryThreshold,
                masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
            )
        )
    }
    val spokenQuestion = remember(question.id) {
        val beforeGap = question.tokens.take(question.verbTokenIndex + 1)
            .joinToString(separator = "") { it.hiragana }
        val afterGap = question.tokens.drop(question.verbTokenIndex + 1)
            .joinToString(separator = "") { it.hiragana }
        beforeGap to afterGap
    }
    LaunchedEffect(question.id) {
        questionSpeech.speakQuestionWithGap(spokenQuestion.first, spokenQuestion.second)
    }

    fun advance() {
        questionIndex++
        wrongAnswers = 0
        onQuestionCompleted(LearningGame.CONJUGATION)
        if (questionIndex < seriesTarget) {
            question = questionDeck.draw(
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Conjugaison", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Question ${questionIndex + 1} / $seriesTarget",
                            color = MaterialTheme.colorScheme.primary
                        )
                        QuestionSpeechButton(questionSpeech, spokenQuestion.first, spokenQuestion.second)
                    }
                }
                if (feedback != null) {
                    AnswerFeedbackBanner(
                        AnswerFeedback(
                            tone = if (feedbackCorrect) FeedbackTone.CORRECT else FeedbackTone.INCORRECT,
                            message = feedback.orEmpty(),
                            pedagogicalHint = feedbackHint
                        ),
                        feedbackEvent
                    )
                }
                Text(
                    question.contextFrench,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    question.translation,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontStyle = FontStyle.Italic
                )
                ConjugationPhrase(
                    question = question,
                    onSelectToken = { selectedToken = it }
                )
            }
        }

        question.choices.chunked(2).forEach { choices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                choices.forEach { ending ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .semantics {
                                contentDescription = "Choisir la terminaison $ending"
                            }
                            .clickable {
                                val correct = ending == question.answerEnding
                                saveError = onAnswer(question, LearningGame.CONJUGATION, correct)
                                if (saveError == null) feedbackEvent++
                                if (saveError == null) {
                                    masteryCounters = masteryCounters + (
                                        question.id to QuestionMasteryRules.updateCounter(
                                            current = masteryCounters[question.id] ?: 0,
                                            wasCorrect = correct
                                        )
                                    )
                                }
                                selectedToken = null
                                if (correct) {
                                    feedbackCorrect = true
                                    feedback = "✓  ${question.completedJapaneseSentence()}"
                                    feedbackHint = concisePedagogicalHint(question.explanation)
                                    advance()
                                } else {
                                    feedbackCorrect = false
                                    val shouldAdvance = ParticleAnswerRules.shouldAdvanceAfterWrongAnswer(
                                        previousWrongAnswers = wrongAnswers,
                                        extraAttempts = extraAttempts
                                    )
                                    wrongAnswers++
                                    feedback = if (shouldAdvance) {
                                        "Réponse : ${question.completedJapaneseSentence()}"
                                    } else {
                                        "Ce n’est pas « $ending ». Réessaie. "
                                    }
                                    feedbackHint = if (shouldAdvance) {
                                        concisePedagogicalHint(question.explanation)
                                    } else {
                                        null
                                    }
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
                                ending,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge
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

    selectedToken?.let { token ->
        Dialog(onDismissRequest = { selectedToken = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF151515),
                border = BorderStroke(1.dp, Color(0xFF393939))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        token.japanese,
                        color = when (token.category) {
                            WordCategory.VERB -> Color(0xFFFFB74D)
                            WordCategory.PARTICLE -> Color(0xFF81C784)
                            WordCategory.GRAMMAR_MARKER -> Color(0xFFCE93D8)
                            WordCategory.VOCABULARY -> Color(0xFF64B5F6)
                            WordCategory.ADJECTIVE -> Color(0xFFFF8A80)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(token.hiragana, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(token.french, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

private fun ConjugationQuestion.completedJapaneseSentence(): String =
    tokens.mapIndexed { index, token ->
        if (index == verbTokenIndex) token.japanese + answerEnding else token.japanese
    }.joinToString(separator = "") + punctuation

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConjugationPhrase(
    question: ConjugationQuestion,
    onSelectToken: (PhraseToken) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally)
    ) {
        question.tokens.forEachIndexed { index, token ->
            val color = when (token.category) {
                WordCategory.VOCABULARY -> Color(0xFF64B5F6)
                WordCategory.PARTICLE -> Color(0xFF81C784)
                WordCategory.VERB -> Color(0xFFFFB74D)
                WordCategory.GRAMMAR_MARKER -> Color(0xFFCE93D8)
                WordCategory.ADJECTIVE -> Color(0xFFFF8A80)
            }
            Text(
                token.japanese,
                color = color,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onSelectToken(token) }
            )
            if (index == question.verbTokenIndex) {
                Text(
                    "...",
                    color = Color(0xFFFFB74D),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics {
                        contentDescription = "Terminaison manquante"
                    }
                )
            }
        }
        Text(" ${question.punctuation}", style = MaterialTheme.typography.headlineSmall)
    }
}
