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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun VocabularyGameScreen(
    game: LearningGame,
    questions: List<VocabularyQuestion>,
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
    var masteryCounters by remember(game, progress) {
        mutableStateOf(progress.vocabularyMasteryCounters())
    }
    var questionIndex by remember { mutableIntStateOf(0) }
    var wrongAnswers by remember { mutableIntStateOf(0) }
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
    val scheduledQuestionMode = VocabularyQuestionMode.forGame(game)
    val questionMode = if (
        scheduledQuestionMode == VocabularyQuestionMode.AUDIO_TO_FRENCH && !questionSpeech.isEnabled
    ) {
        VocabularyQuestionMode.JAPANESE_TO_FRENCH
    } else {
        scheduledQuestionMode
    }
    val displayedChoices = remember(question.id, questionMode, questions) {
        when (questionMode) {
            VocabularyQuestionMode.AUDIO_TO_FRENCH,
            VocabularyQuestionMode.JAPANESE_TO_FRENCH -> {
                val choices = if (question.bank == QuestionBank.COUNTERS) {
                    counterTranslationChoices(question, questions)
                } else {
                    question.choices
                }
                AnswerChoiceRules.four(choices, question.answer)
            }
            VocabularyQuestionMode.FRENCH_TO_JAPANESE -> AnswerChoiceRules.four(
                japaneseAnswerChoices(question, questions),
                question.kanji
            )
            VocabularyQuestionMode.TYPE_JAPANESE -> emptyList()
        }
    }
    LaunchedEffect(question.id, questionMode) {
        if (questionMode == VocabularyQuestionMode.JAPANESE_TO_FRENCH ||
            questionMode == VocabularyQuestionMode.AUDIO_TO_FRENCH
        ) {
            questionSpeech.speakQuestion(question.hiragana)
        }
    }
    var showReading by remember(question.id) { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackTranslation by remember { mutableStateOf<String?>(null) }
    var feedbackHint by remember { mutableStateOf<String?>(null) }
    var feedbackCorrect by remember { mutableStateOf(false) }
    var feedbackEvent by remember { mutableIntStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var typedAnswer by remember(question.id) { mutableStateOf("") }
    fun advance() {
        questionIndex++
        wrongAnswers = 0
        onQuestionCompleted(game)
        if (questionIndex < seriesTarget) {
            question = questionDeck.draw(
                masteryCounters = masteryCounters,
                masteryThreshold = masteryThreshold,
                masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
            )
        }
    }

    fun submitAnswer(response: String) {
        val expected = when (questionMode) {
            VocabularyQuestionMode.AUDIO_TO_FRENCH,
            VocabularyQuestionMode.JAPANESE_TO_FRENCH -> question.answer
            VocabularyQuestionMode.FRENCH_TO_JAPANESE,
            VocabularyQuestionMode.TYPE_JAPANESE -> question.kanji
        }
        val correct = if (questionMode == VocabularyQuestionMode.TYPE_JAPANESE) {
            val normalized = response.filterNot(Char::isWhitespace)
            normalized == question.kanji.filterNot(Char::isWhitespace) ||
                normalized == question.hiragana.filterNot(Char::isWhitespace)
        } else {
            response == expected
        }
        saveError = onAnswer(question, game, correct)
        if (saveError != null) return

        feedbackEvent++
        masteryCounters = masteryCounters + (
            question.id to QuestionMasteryRules.updateCounter(
                current = masteryCounters[question.id] ?: 0,
                wasCorrect = correct,
                threshold = masteryThreshold
            )
        )
        if (correct) {
            feedbackCorrect = true
            feedback = if (questionMode == VocabularyQuestionMode.JAPANESE_TO_FRENCH) "✓" else "✓  ${question.kanji}"
            feedbackTranslation = question.answer
            feedbackHint = vocabularyKanjiBreakdown(question)
            advance()
            return
        }

        feedbackCorrect = false
        val shouldAdvance = ParticleAnswerRules.shouldAdvanceAfterWrongAnswer(
            previousWrongAnswers = wrongAnswers,
            extraAttempts = extraAttempts
        )
        wrongAnswers++
        feedback = when {
            !shouldAdvance -> "Ce n’est pas « $response ». Réessaie."
            questionMode == VocabularyQuestionMode.JAPANESE_TO_FRENCH -> "Réponse : ${question.answer}"
            else -> "Réponse : ${question.kanji}"
        }
        feedbackTranslation = if (shouldAdvance && questionMode != VocabularyQuestionMode.JAPANESE_TO_FRENCH) {
            question.answer
        } else {
            null
        }
        feedbackHint = if (shouldAdvance) vocabularyKanjiBreakdown(question) else null
        if (shouldAdvance) advance()
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

        carryoverAnswer?.let { AnswerFeedbackBanner(it, carryoverAnswerEvent) }
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
                    Text(
                        game.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Question ${questionIndex + 1} / $seriesTarget",
                            color = MaterialTheme.colorScheme.primary
                        )
                        QuestionSpeechButton(
                            speech = questionSpeech,
                            questionText = question.hiragana,
                            speakOnEnable = questionMode == VocabularyQuestionMode.JAPANESE_TO_FRENCH ||
                                questionMode == VocabularyQuestionMode.AUDIO_TO_FRENCH
                        )
                    }
                }
                Text(questionMode.instruction, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (feedback != null) {
                    AnswerFeedbackBanner(
                        AnswerFeedback(
                            tone = if (feedbackCorrect) FeedbackTone.CORRECT else FeedbackTone.INCORRECT,
                            message = feedback.orEmpty(),
                            frenchTranslation = feedbackTranslation,
                            kanjiBreakdown = feedbackHint
                        ),
                        feedbackEvent
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (questionMode == VocabularyQuestionMode.JAPANESE_TO_FRENCH) {
                        Text(
                            question.kanji,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                questionSpeech.speakQuestion(question.hiragana)
                                showReading = true
                            }
                        )
                    } else if (questionMode == VocabularyQuestionMode.AUDIO_TO_FRENCH) {
                        Text(
                            "Écoute le mot",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            question.answer,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFFFD180),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (questionMode == VocabularyQuestionMode.TYPE_JAPANESE) {
            OutlinedTextField(
                value = typedAnswer,
                onValueChange = { typedAnswer = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Réponse en japonais") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Button(
                onClick = { submitAnswer(typedAnswer) },
                enabled = typedAnswer.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05252))
            ) {
                Text("Valider")
            }
        } else {
            displayedChoices.chunked(2).forEach { choices ->
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
                                    if (questionMode == VocabularyQuestionMode.FRENCH_TO_JAPANESE) {
                                        questionSpeech.speakQuestion(choice)
                                    }
                                    submitAnswer(choice)
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
                                    fontStyle = if (questionMode == VocabularyQuestionMode.JAPANESE_TO_FRENCH ||
                                        questionMode == VocabularyQuestionMode.AUDIO_TO_FRENCH
                                    ) {
                                        FontStyle.Italic
                                    } else {
                                        FontStyle.Normal
                                    }
                                )
                            }
                        }
                    }
                    if (choices.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
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

    if (showReading) {
        Dialog(onDismissRequest = { showReading = false }) {
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
                        question.kanji,
                        color = Color(0xFF64B5F6),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        question.hiragana,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
