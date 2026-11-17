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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun CounterChallengeGameScreen(
    questions: List<VocabularyQuestion>,
    questionSpeech: JapaneseQuestionSpeech,
    onBack: () -> Unit,
    onAnswer: (LearningQuestion, LearningGame, Boolean) -> String?,
    onQuestionCompleted: (LearningGame) -> Unit,
    extraAttempts: Int,
    progress: ProgressSnapshot,
    masteryThreshold: Int,
    masteredQuestionRepeatPercent: Int,
    completedQuestions: Int,
    seriesTarget: Int,
    allQuestionsMastered: Boolean,
    carryoverAnswer: AnswerFeedback?,
    carryoverAnswerEvent: Int
) {
    var masteryCounters by remember {
        mutableStateOf(progress.questionsFor(LearningGame.COUNTER_CHALLENGE)
            .associate { it.questionId to it.masteryCounter })
    }
    var wrongAnswers by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<AnswerFeedback?>(null) }
    var feedbackEvent by remember { mutableIntStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val questionDeck = remember(questions) { QuestionDeck(questions.shuffled()) }

    fun drawQuestion(): VocabularyQuestion {
        val drawn = questionDeck.draw(
            masteryCounters = masteryCounters,
            masteryThreshold = masteryThreshold,
            masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
        )
        return drawn.copy(choices = CounterChallengeQuestionRules.randomizedChoices(drawn, questions))
    }

    var question by remember(questionDeck) {
        mutableStateOf(drawQuestion())
    }
    val displayedChoices = remember(question.id) { question.choices.shuffled() }
    val readingByCounter = remember(questions) { questions.associate { it.answer to it.hiragana } }

    fun advance() {
        wrongAnswers = 0
        onQuestionCompleted(LearningGame.COUNTER_CHALLENGE)
        if (completedQuestions + 1 < seriesTarget) {
            question = drawQuestion()
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("‹  Accueil") }
            QuestionSpeechButton(questionSpeech, questionText = "", speakOnEnable = false)
        }
        carryoverAnswer?.let { AnswerFeedbackBanner(it, carryoverAnswerEvent) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = BorderStroke(1.dp, Color(0xFF292929))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Défi des compteurs", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Question ${completedQuestions + 1} / $seriesTarget · ${question.topic}",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Comment dit-on « ${question.kanji} » en japonais ?",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        displayedChoices.chunked(2).forEach { rowChoices ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowChoices.forEach { choice ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clickable {
                                questionSpeech.speakQuestion(readingByCounter[choice].orEmpty())
                                val correct = choice == question.answer
                                saveError = onAnswer(question, LearningGame.COUNTER_CHALLENGE, correct)
                                if (saveError == null) {
                                    masteryCounters = masteryCounters + (
                                        question.id to QuestionMasteryRules.updateCounter(
                                            current = masteryCounters[question.id] ?: 0,
                                            wasCorrect = correct
                                        )
                                    )
                                }
                                feedbackEvent++
                                if (correct) {
                                    feedback = AnswerFeedback(
                                        tone = FeedbackTone.CORRECT,
                                        message = "✓  ${question.answer}",
                                        frenchTranslation = question.kanji,
                                        kanjiBreakdown = kanjiBreakdown(question.answer)
                                    )
                                    advance()
                                } else {
                                    val shouldAdvance = ParticleAnswerRules.shouldAdvanceAfterWrongAnswer(
                                        previousWrongAnswers = wrongAnswers,
                                        extraAttempts = extraAttempts
                                    )
                                    wrongAnswers++
                                    feedback = if (shouldAdvance) {
                                        AnswerFeedback(
                                            tone = FeedbackTone.INCORRECT,
                                            message = "×  Réponse : ${question.answer}",
                                            frenchTranslation = question.kanji,
                                            kanjiBreakdown = kanjiBreakdown(question.answer)
                                        )
                                    } else {
                                        AnswerFeedback(
                                            tone = FeedbackTone.INCORRECT,
                                            message = "Ce n’est pas cette lecture. Réessaie."
                                        )
                                    }
                                    if (shouldAdvance) advance()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
                        border = BorderStroke(1.dp, Color(0xFF292929))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(choice, style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFB74D))
                            Text(
                                readingByCounter[choice].orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowChoices.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        feedback?.let { AnswerFeedbackBanner(it, feedbackEvent) }
        SessionProgressFooter(
            questionsCompleted = completedQuestions,
            target = seriesTarget,
            allQuestionsMastered = allQuestionsMastered
        )
        saveError?.let { Text("Progression non enregistrée : $it", color = MaterialTheme.colorScheme.error) }
    }
}
