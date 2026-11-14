package fr.nihongo.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProgressScreen(
    progress: ProgressSnapshot,
    onBack: () -> Unit
) {
    var selectedGame by rememberSaveable { mutableStateOf(LearningGame.PARTICLES) }
    val questions = progress.questionsFor(selectedGame)
    val topics = progress.topicsFor(selectedGame)
    val attempts = questions.sumOf { it.attempts }
    val correctAnswers = questions.sumOf { it.correctAnswers }
    val accuracy = if (attempts == 0) 0 else correctAnswers * 100 / attempts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Accueil") }
        Text("Ma progression", style = MaterialTheme.typography.headlineSmall)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LearningGame.values().forEach { game ->
                FilterChip(
                    selected = selectedGame == game,
                    onClick = { selectedGame = game },
                    label = { Text(game.title) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = BorderStroke(1.dp, Color(0xFF292929))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${topics.count { it.isMastered }} notion(s) maîtrisée(s)", style = MaterialTheme.typography.titleLarge)
                Text("$attempts réponse(s) enregistrée(s)")
                Text("$accuracy % de réussite")
            }
        }

        Text("Notions à réviser", style = MaterialTheme.typography.titleLarge)
        topics.forEach { topic ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                border = BorderStroke(1.dp, Color(0xFF292929))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(topic.topic, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            when {
                                topic.isMastered -> "Maîtrisée"
                                topic.attempts == 0 -> "À découvrir"
                                else -> "À réviser"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (topic.attempts > 0) {
                            Text("${topic.accuracyPercent} % · ${topic.attempts} réponse(s)")
                        }
                    }
                }
            }
        }

    }
}
