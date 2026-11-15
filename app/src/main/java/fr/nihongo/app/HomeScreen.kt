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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreen(
    progress: ProgressSnapshot,
    selectedGames: List<LearningGame>,
    playableGameCount: Int,
    selectedBanks: Set<QuestionBank>,
    onBankSelectionChange: (QuestionBank, Boolean) -> Unit,
    onGameSelectionChange: (LearningGame, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onStart: () -> Unit,
    onShowProgress: () -> Unit,
    onShowSettings: () -> Unit
) {
    val attempts = LearningGame.values().sumOf { game ->
        progress.questionsFor(game).sumOf { it.attempts }
    }
    val startLabel = when (selectedGames.size) {
        0 -> "Choisis un mini-jeu"
        else -> when (playableGameCount) {
            0 -> "Aucune question à réviser"
            1 -> "Démarrer la séance · 1 jeu"
            else -> "Démarrer la séance · $playableGameCount jeux"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .align(Alignment.BottomCenter)
                )
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("日本語", style = MaterialTheme.typography.headlineMedium, color = HomeAccent)
                    Text("Un peu de japonais,\nchaque jour.", style = MaterialTheme.typography.titleLarge)
                    Text("Choisis tes révisions et c’est parti.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Banques de questions", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (selectedBanks.size == 1) "1 banque sélectionnée" else "${selectedBanks.size} banques sélectionnées",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onShowSettings) { Text("Réglages") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuestionBank.values().forEach { bank ->
                FilterChip(
                    selected = bank in selectedBanks,
                    onClick = { onBankSelectionChange(bank, bank !in selectedBanks) },
                    label = { Text(bank.title) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Mini-jeux", style = MaterialTheme.typography.titleLarge)
                Text("Choisis un ou plusieurs jeux", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                TextButton(onClick = onSelectAll) { Text("Tout") }
                TextButton(onClick = onSelectNone) { Text("Aucun") }
            }
        }

        LearningGame.values().toList().chunked(2).forEach { gameRow ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                gameRow.forEach { game ->
                    val (symbol, label) = when (game) {
                        LearningGame.PARTICLES -> "助" to "Particules"
                        LearningGame.VOCABULARY -> "語" to "Vocabulaire"
                        LearningGame.VERBS -> "動" to "Verbes"
                        LearningGame.CONJUGATION -> "文" to "Conjugaison"
                    }
                    val isSelected = game in selectedGames
                    Card(
                        modifier = Modifier.weight(1f).clickable {
                            onGameSelectionChange(game, !isSelected)
                        },
                        shape = RoundedCornerShape(20.dp),
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
            }
        }

        Button(
            onClick = onStart,
            enabled = playableGameCount > 0,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HomeAccent)
        ) {
            Text(startLabel)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeActionCard("Ma progression", Modifier.weight(1f), onShowProgress)
            HomeActionCard("Réglages", Modifier.weight(1f), onShowSettings)
        }
        Text(
            "$attempts réponses données",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
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
