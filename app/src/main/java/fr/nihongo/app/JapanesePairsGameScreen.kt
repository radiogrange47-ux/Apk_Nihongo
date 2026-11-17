package fr.nihongo.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun JapanesePairsGameScreen(
    questions: List<VocabularyQuestion>,
    questionSpeech: JapaneseQuestionSpeech,
    onBack: () -> Unit,
    onAnswer: (LearningQuestion, LearningGame, Boolean) -> String?,
    onQuestionCompleted: (LearningGame) -> Unit,
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
        mutableStateOf(progress.questionsFor(LearningGame.PAIRS).associate { it.questionId to it.masteryCounter })
    }
    val seenIds = remember(questions) { mutableSetOf<String>() }

    fun drawBoard(): List<VocabularyQuestion> {
        var available = questions.filter { it.id !in seenIds }
        if (available.size < 6) {
            seenIds.clear()
            available = questions
        }
        if (masteredQuestionRepeatPercent == 0) {
            available = available.filter { (masteryCounters[it.id] ?: 0) < masteryThreshold }
        }
        require(available.size >= 6) { "Il faut six paires encore révisables pour commencer une grille." }

        val selected = mutableListOf<VocabularyQuestion>()
        repeat(6) {
            val remaining = available.filterNot { candidate -> selected.any { it.id == candidate.id } }
            val question = QuestionSelector.draw(
                questions = remaining,
                masteryCounters = masteryCounters,
                masteryThreshold = masteryThreshold,
                count = 1,
                masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
            ).single()
            selected += question
            seenIds += question.id
        }
        return selected
    }

    var board by remember(questions) { mutableStateOf(drawBoard()) }
    val frenchColumn = remember(board) { board.shuffled() }
    var selectedJapaneseId by remember(board) { mutableStateOf<String?>(null) }
    var selectedFrenchId by remember(board) { mutableStateOf<String?>(null) }
    var matchedIds by remember(board) { mutableStateOf(emptySet<String>()) }
    var wrongPair by remember(board) { mutableStateOf<Pair<String, String>?>(null) }
    var saveError by remember(board) { mutableStateOf<String?>(null) }
    var lastMatchedQuestion by remember { mutableStateOf<VocabularyQuestion?>(null) }
    var previousAnswer by remember { mutableStateOf<AnswerFeedback?>(null) }
    var previousAnswerEvent by remember { mutableIntStateOf(0) }

    fun tryPair(japaneseId: String, frenchId: String) {
        if (matchedIds.size >= 6 || japaneseId in matchedIds || frenchId in matchedIds) return
        val question = board.firstOrNull { it.id == japaneseId } ?: return
        val correct = japaneseId == frenchId
        saveError = onAnswer(question, LearningGame.PAIRS, correct)
        if (saveError == null) {
            masteryCounters = masteryCounters + (
                question.id to QuestionMasteryRules.updateCounter(
                    current = masteryCounters[question.id] ?: 0,
                    wasCorrect = correct
                )
            )
        }
        if (correct) {
            matchedIds = matchedIds + japaneseId
            lastMatchedQuestion = question
            selectedJapaneseId = null
            selectedFrenchId = null
        } else {
            wrongPair = japaneseId to frenchId
        }
    }

    LaunchedEffect(wrongPair) {
        if (wrongPair != null) {
            delay(500)
            selectedJapaneseId = null
            selectedFrenchId = null
            wrongPair = null
        }
    }

    LaunchedEffect(matchedIds.size) {
        if (matchedIds.size == 6) {
            delay(700)
            lastMatchedQuestion?.let { question ->
                previousAnswer = AnswerFeedback(
                    tone = FeedbackTone.CORRECT,
                    message = "✓ Réponse précédente : ${question.kanji} (${question.hiragana})",
                    frenchTranslation = question.answer,
                    kanjiBreakdown = kanjiBreakdown(question.kanji)
                )
                previousAnswerEvent++
            }
            onQuestionCompleted(LearningGame.PAIRS)
            if (completedQuestions + 1 < seriesTarget) {
                lastMatchedQuestion = null
                board = drawBoard()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("‹  Accueil") }
            QuestionSpeechButton(
                speech = questionSpeech,
                questionText = board.first().hiragana,
                speakOnEnable = false
            )
        }
        carryoverAnswer?.let { AnswerFeedbackBanner(it, carryoverAnswerEvent) }
        Text("Paires japonaises", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Grille ${completedQuestions + 1} / $seriesTarget · Associe les six mots et leur traduction.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        previousAnswer?.let { AnswerFeedbackBanner(it, previousAnswerEvent) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                board.forEach { question ->
                    val matched = question.id in matchedIds
                    val selected = selectedJapaneseId == question.id
                    val incorrect = wrongPair?.first == question.id
                    PairCard(
                        text = question.kanji,
                        reading = question.hiragana,
                        matched = matched,
                        selected = selected,
                        incorrect = incorrect,
                        onClick = {
                            if (!matched && wrongPair == null) {
                                questionSpeech.speakQuestion(question.hiragana)
                                selectedJapaneseId = question.id
                                selectedFrenchId?.let { tryPair(question.id, it) }
                            }
                        }
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frenchColumn.forEach { question ->
                    val matched = question.id in matchedIds
                    val selected = selectedFrenchId == question.id
                    val incorrect = wrongPair?.second == question.id
                    PairCard(
                        text = question.answer,
                        matched = matched,
                        selected = selected,
                        incorrect = incorrect,
                        onClick = {
                            if (!matched && wrongPair == null) {
                                selectedFrenchId = question.id
                                selectedJapaneseId?.let { tryPair(it, question.id) }
                            }
                        }
                    )
                }
            }
        }
        if (matchedIds.size == 6) {
            Text(
                "✓ Six paires trouvées !",
                color = Color(0xFF8EE6B5),
                style = MaterialTheme.typography.titleMedium
            )
        } else if (wrongPair != null) {
            Text("Pas cette paire, essaie encore.", color = Color(0xFFFF9999))
        }
        saveError?.let { Text("Progression non enregistrée : $it", color = MaterialTheme.colorScheme.error) }
        SessionProgressFooter(
            questionsCompleted = completedQuestions,
            target = seriesTarget,
            allQuestionsMastered = allQuestionsMastered
        )
        Spacer(Modifier.heightIn(min = 4.dp))
    }
}

@Composable
private fun PairCard(
    text: String,
    reading: String? = null,
    matched: Boolean,
    selected: Boolean,
    incorrect: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        matched -> Color(0xFF12352A)
        incorrect -> Color(0xFF3A1717)
        selected -> Color(0xFF241719)
        else -> Color(0xFF141414)
    }
    val border = when {
        matched -> Color(0xFF8EE6B5)
        incorrect -> Color(0xFFFF9999)
        selected -> Color(0xFFE05252)
        else -> Color(0xFF292929)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(enabled = !matched, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            reading?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
