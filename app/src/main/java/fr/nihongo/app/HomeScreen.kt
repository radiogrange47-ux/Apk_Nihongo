package fr.nihongo.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val HomeAccent = Color(0xFFE05252)
private val HomePanel = Color(0xFF141414)
private val HomeBorder = Color(0xFF292929)

private data class BankMasterySummary(val mastered: Int, val learning: Int, val percent: Int)

@Composable
internal fun HomeScreen(
    progress: ProgressSnapshot,
    questionCatalog: QuestionCatalog,
    masteryThreshold: Int,
    appVersion: String,
    selectedTrack: LearningTrack,
    onTrackSelectionChange: (LearningTrack) -> Unit,
    onShowProgress: () -> Unit,
    onShowSettings: () -> Unit
) {
    val trackProgress = remember(questionCatalog, progress, masteryThreshold) {
        LearningTrack.values().associateWith { track ->
            LearningConceptProgress.from(
                catalog = track.catalog(questionCatalog),
                progress = progress,
                masteryThreshold = masteryThreshold,
                includeAuxiliaryModes = track == LearningTrack.VOCABULARY
            )
        }
    }
    val attempts = progress.questions.sumOf { it.attempts }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        HomeBanner()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Choisis ton parcours", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tu choisiras ensuite tes banques de questions et tes jeux.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LearningTrack.values().forEach { track ->
                val summary = trackProgress.getValue(track)
                val selected = track == selectedTrack
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onTrackSelectionChange(track) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFF241719) else HomePanel
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) HomeAccent.copy(alpha = 0.75f) else HomeBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(track.symbol, color = HomeAccent, style = MaterialTheme.typography.headlineSmall)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium)
                            Text(track.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${summary.mastered} acquises", color = HomeAccent)
                            Text("${summary.learning + summary.discovered + summary.nearlyReady} en cours · ${summary.total} notions", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeActionCard("Ma progression", Modifier.weight(1f), onShowProgress)
            HomeActionCard("Réglages", Modifier.weight(1f), onShowSettings)
        }
        Text(
            "Version $appVersion · $attempts réponses données",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackSetupScreen(
    track: LearningTrack,
    questionCatalog: QuestionCatalog,
    progress: ProgressSnapshot,
    masteryThreshold: Int,
    availableBanks: List<QuestionBank>,
    selectedGames: List<LearningGame>,
    playableGameCount: Int,
    activeNotionCount: Int,
    remainingNotionCount: Int,
    selectedBanks: Set<QuestionBank>,
    onBack: () -> Unit,
    onBankSelectionChange: (QuestionBank, Boolean) -> Unit,
    onGameSelectionChange: (LearningGame, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onStart: () -> Unit
) {
    val masteredCounters = remember(progress) {
        progress.questions
            .groupBy { it.questionId }
            .mapValues { (_, rows) -> rows.maxOf { it.masteryCounter } }
    }
    val attemptsByQuestion = remember(progress) {
        progress.questions.groupBy { it.questionId }.mapValues { (_, rows) -> rows.sumOf { it.attempts } }
    }
    val vocabularyMastery = remember(progress) { progress.vocabularyMasteryCounters() }
    val pairIdsByMeaning = remember(questionCatalog) {
        questionCatalog.pairingQuestions().associate {
            Triple(it.kanji, it.hiragana, it.answer) to it.id
        }
    }
    val bankMastery = availableBanks.associateWith { bank ->
        val questions = questionCatalog.questionsForBank(bank)
        val counters = questions.map { question ->
            val directCounter = if (question is VocabularyQuestion) {
                vocabularyMastery[question.id] ?: 0
            } else {
                masteredCounters[question.id] ?: 0
            }
            val pairId = (question as? VocabularyQuestion)?.let {
                pairIdsByMeaning[Triple(it.kanji, it.hiragana, it.answer)]
            }
            maxOf(directCounter, pairId?.let { masteredCounters[it] ?: 0 } ?: 0)
        }
        val stages = questions.mapIndexed { index, question ->
            val pairId = (question as? VocabularyQuestion)?.let {
                pairIdsByMeaning[Triple(it.kanji, it.hiragana, it.answer)]
            }
            val attempts = (attemptsByQuestion[question.id] ?: 0) +
                (pairId?.let { attemptsByQuestion[it] ?: 0 } ?: 0)
            MasteryStage.from(counters[index], masteryThreshold, attempts)
        }
        BankMasterySummary(
            mastered = stages.count { it == MasteryStage.MASTERED },
            learning = stages.count { it == MasteryStage.FIRST_WIN || it == MasteryStage.LEARNING || it == MasteryStage.NEARLY_READY },
            percent = if (stages.isEmpty()) 0 else (stages.sumOf {
                when (it) {
                    MasteryStage.DISCOVER -> 0.0
                    MasteryStage.FIRST_WIN -> 0.25
                    MasteryStage.LEARNING -> 0.5
                    MasteryStage.NEARLY_READY -> 0.75
                    MasteryStage.MASTERED -> 1.0
                }
            } * 100 / stages.size).toInt()
        )
    }
    val gamePresentation = mapOf(
        LearningGame.PARTICLES to ("助" to "Particules"),
        LearningGame.VOCABULARY to ("語" to "Japonais → français"),
        LearningGame.VOCABULARY_AUDIO to ("聞" to "Écoute et traduction"),
        LearningGame.VOCABULARY_FRENCH_TO_JAPANESE to ("訳" to "Français → japonais"),
        LearningGame.VOCABULARY_WRITE to ("書" to "Écrire en japonais"),
        LearningGame.VERBS to ("動" to "Verbes"),
        LearningGame.CONJUGATION to ("文" to "Conjugaison"),
        LearningGame.PAIRS to ("組" to "Paires")
    )
    val startLabel = when {
        selectedGames.isEmpty() -> "Choisis un mini-jeu"
        playableGameCount == 0 -> "Aucune question disponible"
        else -> "Démarrer la séance · $playableGameCount jeu${if (playableGameCount > 1) "x" else ""}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Menu principal") }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.title, style = MaterialTheme.typography.headlineMedium, color = HomeAccent)
            Text(track.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Banques de questions", style = MaterialTheme.typography.titleLarge)
            Text(
                if (selectedBanks.intersect(availableBanks.toSet()).size == 1) "1 banque sélectionnée"
                else "${selectedBanks.intersect(availableBanks.toSet()).size} banques sélectionnées",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (availableBanks.isEmpty()) {
                Text("Aucune banque installée pour ce parcours.", color = MaterialTheme.colorScheme.error)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableBanks.forEach { bank ->
                        val mastery = bankMastery.getValue(bank)
                        FilterChip(
                            selected = bank in selectedBanks,
                            onClick = { onBankSelectionChange(bank, bank !in selectedBanks) },
                            label = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(bank.title)
                                    Text(
                                        "${mastery.percent}% - ${mastery.mastered} acquises - ${mastery.learning} en cours",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mini-jeux", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$activeNotionCount notion(s) dans le groupe actif sur ${ActiveLearningPool.MAX_ACTIVE_NOTIONS} - $remainingNotionCount à apprendre",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Choisis les jeux de cette séance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    TextButton(onClick = onSelectAll) { Text("Tout") }
                    TextButton(onClick = onSelectNone) { Text("Aucun") }
                }
            }
            track.gamesIn(questionCatalog).chunked(2).forEach { rowGames ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowGames.forEach { game ->
                        val (symbol, label) = gamePresentation.getValue(game)
                        val isSelected = game in selectedGames
                        Card(
                            modifier = Modifier.weight(1f).clickable {
                                onGameSelectionChange(game, !isSelected)
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF241719) else HomePanel
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) HomeAccent.copy(alpha = 0.65f) else HomeBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onGameSelectionChange(game, it) }
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(symbol, color = HomeAccent, style = MaterialTheme.typography.titleLarge)
                                    Text(label, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                    if (rowGames.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }

        Button(
            onClick = onStart,
            enabled = playableGameCount > 0,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HomeAccent)
        ) { Text(startLabel) }
    }
}

@Composable
private fun HomeBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
            Image(
                painter = painterResource(R.drawable.nihongo_home_banner),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxWidth().height(112.dp).align(Alignment.BottomCenter)
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("日本語", style = MaterialTheme.typography.headlineMedium, color = HomeAccent)
                Text("Un peu de japonais,\nchaque jour.", style = MaterialTheme.typography.titleLarge)
                Text("Choisis un parcours et c’est parti.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeActionCard(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HomePanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, HomeBorder)
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
